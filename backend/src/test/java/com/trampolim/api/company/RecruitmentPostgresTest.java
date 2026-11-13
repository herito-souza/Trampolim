package com.trampolim.api.company;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.MediaType;
import com.trampolim.api.BackendApplication;
import com.trampolim.api.security.TrailTokenService;

@SpringBootTest(classes=BackendApplication.class,properties="radar.bridge-secret=synthetic-application-center-test-secret")
class RecruitmentPostgresTest {
    @Autowired RecruitmentService service;
    @Autowired CandidateApplicationCenterController applicationCenter;
    @Autowired RecruitmentRetention retention;
    @Autowired CompanyController legacy;
    @Autowired WebApplicationContext context;
    @Autowired TrailTokenService tokens;
    @Autowired @Qualifier("empresasJdbcTemplate") JdbcTemplate jdbc;
    record Fixture(String owner,long user,long company,long listing,long application){}
    Fixture fixture(){
        String suffix=UUID.randomUUID().toString(),email="recruitment-"+suffix+"@example.test";
        long user=jdbc.queryForObject("INSERT INTO company_users(email,password_hash,full_name,email_verified,account_status) VALUES (?,'fixture-not-a-password','Recrutador sintético',TRUE,'ACTIVE') RETURNING id",Long.class,email);
        String cnpj=String.format("%014d",Math.abs(UUID.randomUUID().getMostSignificantBits()%100000000000000L));
        long company=jdbc.queryForObject("INSERT INTO companies(owner_user_id,legal_name,trade_name,cnpj,phone,industry,address,city,state,verification_status) VALUES (?,'Empresa de teste isolada','Empresa de teste isolada',?,'fixture','fixture','fixture','Irecê','BA','APPROVED') RETURNING id",Long.class,user,cnpj);
        long listing=listing(company);
        long application=application(listing,"candidato-"+suffix+"@example.test");
        return new Fixture(email,user,company,listing,application);
    }
    long listing(long company){return jdbc.queryForObject("INSERT INTO company_listings(company_id,kind,title,area,description,requirements,location,modality,url,contact_email,payment_status,publication_status) VALUES (?,'job','Vaga sintética isolada','fixture','fixture','fixture','Irecê, BA','Presencial','https://example.test','fixture@example.test','NOT_PROCESSED','DRAFT') RETURNING id",Long.class,company);}
    long application(long listing,String email){return jdbc.queryForObject("INSERT INTO company_applications(listing_id,candidate_name,candidate_email) VALUES (?,'Pessoa sintética',?) RETURNING id",Long.class,listing,email);}
    String member(Fixture f,String role){String email=UUID.randomUUID()+"@example.test";long user=jdbc.queryForObject("INSERT INTO company_users(email,password_hash,full_name,email_verified,account_status) VALUES (?,'fixture','Equipe sintética',TRUE,'ACTIVE') RETURNING id",Long.class,email);jdbc.update("INSERT INTO company_members(company_id,user_id,role) VALUES (?,?,?)",f.company(),user,role);return email;}
    void expectStatus(int expected,Runnable run){assertEquals(expected,assertThrows(ResponseStatusException.class,run::run).getStatusCode().value());}
    RecruitmentService.Change change(String state,long rev){return new RecruitmentService.Change(state,rev,UUID.randomUUID(),false);}
    @Test void stagesHistoryAndLegacyEndpointShareTheSameApplication(){
        var f=fixture();assertEquals("RECEIVED",service.history(f.owner(),f.application(),0,25).items().getFirst().eventType());
        long revision=0;for(String stage:List.of("IN_REVIEW","CONTACTED","INTERVIEW","ACCEPTED","DECLINED","CLOSED")){
            var result=service.change(f.owner(),f.application(),change(stage,revision));assertEquals(++revision,result.revision());
        }
        assertEquals(7,service.history(f.owner(),f.application(),0,25).totalElements());
        expectStatus(409,()->service.change(f.owner(),f.application(),change("IN_REVIEW",6)));
        service.change(f.owner(),f.application(),new RecruitmentService.Change("IN_REVIEW",6L,UUID.randomUUID(),true));
        assertEquals("ACCEPTED",legacy.applicationStatus(f.owner(),f.application(),new CompanyController.StatusInput("ACCEPTED")).get("status"));
        assertEquals(8,service.application(f.owner(),f.application()).revision());
        assertEquals(9,service.history(f.owner(),f.application(),0,25).totalElements());
    }
    @Test void duplicateRequestsAreIdempotentAndStaleChangesAreRejected(){
        var f=fixture();var input=change("INTERVIEW",0);var first=service.change(f.owner(),f.application(),input);
        assertTrue(service.change(f.owner(),f.application(),input).replayed());
        expectStatus(409,()->service.change(f.owner(),f.application(),new RecruitmentService.Change("ACCEPTED",0L,input.requestKey(),false)));
        expectStatus(409,()->service.change(f.owner(),f.application(),change("DECLINED",0)));
        assertEquals(first.revision(),service.application(f.owner(),f.application()).revision());
        assertEquals(2,service.history(f.owner(),f.application(),0,25).totalElements());
    }
    @Test void notesArePrivateVersionedRemovableAndDoNotLeakIntoLegacyViews()throws Exception{
        var f=fixture();var note=new RecruitmentService.Note("Observação interna sintética",0L,UUID.randomUUID());
        var added=service.addNote(f.owner(),f.application(),note);assertTrue(service.addNote(f.owner(),f.application(),note).replayed());
        assertEquals(1,service.notes(f.owner(),f.application(),0,25).totalElements());
        assertFalse(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(legacy.applications(f.owner())).contains("Observação interna"));
        assertFalse(service.history(f.owner(),f.application(),0,25).toString().contains("Observação interna"));
        expectStatus(409,()->service.addNote(f.owner(),f.application(),new RecruitmentService.Note("Outro texto",0L,note.requestKey())));
        var input=new RecruitmentService.Remove(1L,UUID.randomUUID());service.removeNote(f.owner(),f.application(),added.noteId(),input);
        assertTrue(service.removeNote(f.owner(),f.application(),added.noteId(),input).replayed());
        assertEquals(0,service.notes(f.owner(),f.application(),0,25).totalElements());
        assertNull(jdbc.queryForObject("SELECT body FROM company_recruitment_notes WHERE id=?",String.class,added.noteId()));
    }
    @Test void tenantAndRoleBoundariesApplyToEveryResource(){
        var a=fixture();var b=fixture();String recruiter=member(a,"RECRUITER");
        for(String role:List.of("ANALYST","ADMIN")){String email=member(a,role);expectStatus(403,()->service.applications(email,null,null,0,25));expectStatus(403,()->service.notes(email,a.application(),0,25));expectStatus(403,()->service.change(email,a.application(),change("INTERVIEW",0)));}
        assertEquals(1,service.applications(recruiter,null,null,0,25).totalElements());
        expectStatus(404,()->service.application(a.owner(),b.application()));expectStatus(404,()->service.history(a.owner(),b.application(),0,25));
        expectStatus(404,()->service.notes(a.owner(),b.application(),0,25));expectStatus(404,()->service.change(a.owner(),b.application(),change("INTERVIEW",0)));
        expectStatus(404,()->service.addNote(a.owner(),b.application(),new RecruitmentService.Note("Não autorizado",0L,UUID.randomUUID())));
        expectStatus(404,()->service.removeNote(a.owner(),b.application(),UUID.randomUUID(),new RecruitmentService.Remove(0L,UUID.randomUUID())));
        expectStatus(404,()->service.applications(a.owner(),b.listing(),null,0,25));expectStatus(404,()->service.indicators(a.owner(),b.listing(),null));
        jdbc.update("UPDATE companies SET verification_status='SUSPENDED' WHERE id=?",a.company());expectStatus(403,()->service.applications(a.owner(),null,null,0,25));
    }
    @Test void stablePaginationCombinedFiltersAndIndicatorsUseOnlyAuthorizedData(){
        var f=fixture();fixture();long second=listing(f.company());for(int n=0;n<5;n++)application(f.listing(),"pagination-"+n+"@example.test");application(second,"other-listing@example.test");
        service.change(f.owner(),f.application(),change("INTERVIEW",0));
        var page=service.applications(f.owner(),f.listing(),null,0,2);assertEquals(6,page.totalElements());assertTrue(page.hasNext());
        var next=service.applications(f.owner(),f.listing(),null,1,2);assertTrue(Collections.disjoint(page.items().stream().map(RecruitmentService.Application::id).toList(),next.items().stream().map(RecruitmentService.Application::id).toList()));
        assertEquals(1,service.applications(f.owner(),f.listing(),"INTERVIEW",0,25).totalElements());
        var metrics=service.indicators(f.owner(),f.listing(),null);assertEquals(6,metrics.total());assertEquals(5,metrics.awaitingReview());assertEquals(1,metrics.inProgress());assertEquals(0,metrics.selected());
        assertEquals(0,service.applications(f.owner(),null,"CLOSED",0,25).totalElements());
        expectStatus(400,()->service.applications(f.owner(),null,null,-1,25));expectStatus(400,()->service.applications(f.owner(),null,null,0,101));
        expectStatus(400,()->service.applications(f.owner(),null,"IN_REVIEW' OR 1=1",0,25));expectStatus(400,()->service.change(f.owner(),f.application(),new RecruitmentService.Change("INTERVIEW",null,null,false)));
    }
    @Test void candidateTimelineUsesVerifiedAddressScopeAndNeverReturnsInternalNotes(){
        var f=fixture();String email="verified-candidate-"+UUID.randomUUID()+"@example.test";long application=application(f.listing(),email);
        service.change(f.owner(),application,change("IN_REVIEW",0));
        service.addNote(f.owner(),application,new RecruitmentService.Note("Observação interna confidencial",1L,UUID.randomUUID()));
        service.change(f.owner(),application,change("INTERVIEW",2));
        var result=applicationCenter.query("synthetic-application-center-test-secret",new CandidateApplicationCenterController.Query(email,"INTERVIEW","teste isolada","sintética isolada",null,null,0,10));
        assertEquals(1,result.totalElements());var item=result.items().getFirst();assertEquals("INTERVIEW",item.status());assertEquals(3,item.timeline().size());assertEquals("RECEIVED",item.timeline().getFirst().status());assertEquals("INTERVIEW",item.timeline().getLast().status());
        var serialized=new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules().valueToTree(item).toString();assertFalse(serialized.contains("Observação interna confidencial"));assertFalse(serialized.contains(email));assertFalse(serialized.contains("phone"));
        service.change(f.owner(),application,change("CLOSED",3));var closed=applicationCenter.query("synthetic-application-center-test-secret",new CandidateApplicationCenterController.Query(email,null,null,null,null,null,0,10)).items().getFirst();assertEquals("CLOSED",closed.status());assertEquals("CLOSED",closed.timeline().getLast().status());
        assertEquals(0,applicationCenter.query("synthetic-application-center-test-secret",new CandidateApplicationCenterController.Query(email,"DECLINED",null,null,null,null,0,10)).totalElements());
        assertEquals(0,applicationCenter.query("synthetic-application-center-test-secret",new CandidateApplicationCenterController.Query("other-"+email,null,null,null,null,null,0,10)).totalElements());
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->applicationCenter.query("wrong-secret",new CandidateApplicationCenterController.Query(email,null,null,null,null,null,0,10)));
    }
    @Test void concurrentRecruitersCannotOverwriteTheSameRevision()throws Exception{
        var f=fixture();String colleague=member(f,"RECRUITER");var ready=new CountDownLatch(2);var start=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)){
            var results=new ArrayList<Future<Integer>>();for(String email:List.of(f.owner(),colleague))results.add(pool.submit(()->{ready.countDown();assertTrue(start.await(5,TimeUnit.SECONDS));try{service.change(email,f.application(),change(email.equals(f.owner())?"INTERVIEW":"DECLINED",0));return 200;}catch(ResponseStatusException expected){return expected.getStatusCode().value();}}));
            assertTrue(ready.await(5,TimeUnit.SECONDS));start.countDown();var statuses=new ArrayList<Integer>();for(var result:results)statuses.add(result.get(10,TimeUnit.SECONDS));Collections.sort(statuses);assertEquals(List.of(200,409),statuses);
        }
        assertEquals(1,service.application(f.owner(),f.application()).revision());assertEquals(2,service.history(f.owner(),f.application(),0,25).totalElements());
    }
    @Test void failingAuditRollsBackTheStageAndRevision(){
        var f=fixture();String constraint="test_recruitment_"+UUID.randomUUID().toString().replace("-","");
        jdbc.execute("ALTER TABLE company_recruitment_events ADD CONSTRAINT "+constraint+" CHECK (application_id<>"+f.application()+" OR revision=0)");
        try{assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->service.change(f.owner(),f.application(),change("INTERVIEW",0)));assertEquals(0,service.application(f.owner(),f.application()).revision());assertEquals("RECEIVED",service.application(f.owner(),f.application()).status());}
        finally{jdbc.execute("ALTER TABLE company_recruitment_events DROP CONSTRAINT "+constraint);}
    }
    @Test void retentionHidesExpiredNotesAndPurgesOnlyEligibleNewRecruitmentData(){
        var f=fixture();var saved=service.addNote(f.owner(),f.application(),new RecruitmentService.Note("Texto expirado sintético",0L,UUID.randomUUID()));
        jdbc.update("UPDATE company_recruitment_notes SET expires_at=CURRENT_TIMESTAMP-INTERVAL '1 day' WHERE id=?",saved.noteId());
        assertEquals(0,service.notes(f.owner(),f.application(),0,25).totalElements());retention.purge();
        assertNull(jdbc.queryForObject("SELECT body FROM company_recruitment_notes WHERE id=?",String.class,saved.noteId()));
        assertEquals(1,service.application(f.owner(),f.application()).revision());assertEquals(2,service.history(f.owner(),f.application(),0,25).totalElements());
        jdbc.update("UPDATE company_recruitment_events SET created_at=CURRENT_TIMESTAMP-INTERVAL '731 days' WHERE application_id=? AND revision>0",f.application());retention.purge();
        assertEquals(1,service.history(f.owner(),f.application(),0,25).totalElements());
    }
    @Test void httpEndpointsRequireTheCorporateSessionAndNeverReturnNotesInPublicContracts()throws Exception{
        var f=fixture();var mvc=MockMvcBuilders.webAppContextSetup(context).apply(SecurityMockMvcConfigurers.springSecurity()).build();
        String token=tokens.issue(TrailTokenService.Trail.EMPRESAS,f.owner(),f.user(),0);
        mvc.perform(get("/api/company/recruitment/stages").header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("$[2].code").value("INTERVIEW")).andExpect(jsonPath("$[5].code").value("CLOSED"));
        mvc.perform(get("/api/company/recruitment/applications").header("Authorization","Bearer "+token).param("listingId",Long.toString(f.listing())).param("size","1")).andExpect(status().isOk()).andExpect(jsonPath("$.items[0].id").value(f.application())).andExpect(jsonPath("$.items[0].revision").value(0)).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(put("/api/company/recruitment/applications/"+f.application()+"/stage").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(Map.of("status","INTERVIEW","expectedRevision",0,"requestKey",UUID.randomUUID().toString())))).andExpect(status().isOk()).andExpect(jsonPath("$.revision").value(1));
        for(var trail:List.of(TrailTokenService.Trail.OPORTUNIDADES,TrailTokenService.Trail.CAPACITA)){
            var result=mvc.perform(get("/api/company/recruitment/applications").header("Authorization","Bearer "+tokens.issue(trail,f.owner(),f.user(),0))).andReturn();assertTrue(Set.of(401,403).contains(result.getResponse().getStatus()));
        }
        var anonymous=mvc.perform(get("/api/company/recruitment/applications")).andReturn();assertTrue(Set.of(401,403).contains(anonymous.getResponse().getStatus()));
        UUID request=UUID.randomUUID();
        var created=mvc.perform(post("/api/company/recruitment/applications/"+f.application()+"/notes").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(Map.of("body","Nota privada via API","expectedRevision",1,"requestKey",request.toString())))).andExpect(status().isOk()).andExpect(jsonPath("$.revision").value(2)).andReturn();
        String note=new com.fasterxml.jackson.databind.ObjectMapper().readTree(created.getResponse().getContentAsString()).path("noteId").asText();
        mvc.perform(get("/api/company/recruitment/applications/"+f.application()+"/notes").header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("$.items[0].body").value("Nota privada via API"));
        mvc.perform(get("/api/company/recruitment/applications/"+f.application()+"/history").header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(3));
        mvc.perform(delete("/api/company/recruitment/applications/"+f.application()+"/notes/"+note).header("Authorization","Bearer "+token).param("expectedRevision","2").param("requestKey",UUID.randomUUID().toString())).andExpect(status().isOk()).andExpect(jsonPath("$.revision").value(3));
        // Existing clients can still send status alone; the controller records its change in the same audit trail.
        mvc.perform(put("/api/company/applications/"+f.application()+"/status").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(Map.of("status","ACCEPTED")))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACCEPTED"));
        assertEquals(4,service.application(f.owner(),f.application()).revision());
    }
}
