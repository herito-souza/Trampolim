package com.trampolim.api.company;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@ConditionalOnProperty(name="trampolim.datasource.empresas.enabled",havingValue="true")
public class RecruitmentService {
    public static final List<String> STAGES=List.of("RECEIVED","IN_REVIEW","INTERVIEW","ACCEPTED","DECLINED","CLOSED","CONTACTED");
    private final JdbcTemplate jdbc;
    @org.springframework.beans.factory.annotation.Autowired(required=false) private com.trampolim.api.events.DomainEventOutbox domainEvents;
    private final int notesDays;
    public RecruitmentService(@Qualifier("empresasJdbcTemplate") JdbcTemplate jdbc,
            @Value("${recruitment.notes-retention-days:180}") int notesDays) {
        if(notesDays<1||notesDays>3650) throw new IllegalArgumentException("Invalid recruitment notes retention");
        this.jdbc=jdbc; this.notesDays=notesDays;
    }
    public record Actor(long userId,long companyId) {}
    public record Stage(String code,String label,boolean legacy) {}
    public List<Stage> stages(String email){
        actor(email);
        return List.of(new Stage("RECEIVED","Recebido",false),new Stage("IN_REVIEW","Em análise",false),new Stage("INTERVIEW","Entrevista",false),new Stage("ACCEPTED","Selecionado",false),new Stage("DECLINED","Não selecionado",false),new Stage("CLOSED","Encerrado",false),new Stage("CONTACTED","Contatado (legado)",true));
    }
    public record Change(String status,Long expectedRevision,UUID requestKey,Boolean reopen) {}
    public record Note(String body,Long expectedRevision,UUID requestKey) {}
    public record Remove(Long expectedRevision,UUID requestKey) {}
    public record Mutation(String status,long revision,UUID noteId,boolean replayed) {}
    public record Application(long id,long listingId,String listingTitle,String kind,String name,String email,
            String phone,String message,String status,long revision,Instant createdAt,Instant updatedAt) {}
    public record Event(long id,long revision,String eventType,String previousStatus,String status,
            Long actorUserId,UUID noteId,Instant createdAt) {}
    public record InternalNote(UUID id,String body,Long authorUserId,Instant createdAt,Instant expiresAt) {}
    public record Page<T>(List<T> items,int page,int size,long totalElements,boolean hasNext) {}
    public record Indicators(long total,Map<String,Long> byStage,long awaitingReview,long inProgress,long selected,
            String notice) {}
    private record Current(String status,long revision) {}
    private record Filter(String sql,List<Object> args) {}

    // Same owner fallback and privileges as the existing corporate panel. Never trust companyId from a request.
    Actor actor(String email) {
        var rows=jdbc.queryForList("""
          SELECT u.id AS user_id,x.company_id,x.role,c.verification_status
          FROM company_users u JOIN LATERAL (
            SELECT c.id AS company_id,'OWNER'::varchar AS role,0 AS priority FROM companies c WHERE c.owner_user_id=u.id
            UNION ALL SELECT m.company_id,m.role,1 FROM company_members m WHERE m.user_id=u.id
          ) x ON TRUE JOIN companies c ON c.id=x.company_id
          WHERE LOWER(u.email)=LOWER(?) AND u.account_status='ACTIVE' AND u.email_verified=TRUE
          ORDER BY x.priority,CASE x.role WHEN 'OWNER' THEN 0 ELSE 1 END,x.company_id LIMIT 1
          """,email);
        if(rows.isEmpty()) forbidden();
        var row=rows.getFirst();
        if(!Set.of("OWNER","RECRUITER").contains(row.get("role"))||"SUSPENDED".equals(row.get("verification_status"))) forbidden();
        return new Actor(((Number)row.get("user_id")).longValue(),((Number)row.get("company_id")).longValue());
    }
    private void forbidden(){throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Seu perfil não tem acesso ao recrutamento desta empresa.");}
    private Filter filter(Actor actor,Long listingId,String stage) {
        var args=new ArrayList<Object>(); args.add(actor.companyId());
        String sql=" FROM company_applications a JOIN company_listings l ON l.id=a.listing_id WHERE l.company_id=?";
        if(listingId!=null){if(listingId<1)bad("Vaga inválida.");ownListing(actor,listingId);sql+=" AND a.listing_id=?";args.add(listingId);}
        if(stage!=null){stage(stage);sql+=" AND a.status=?";args.add(stage);}
        return new Filter(sql,args);
    }
    private void ownListing(Actor actor,long id){
        if(!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM company_listings WHERE id=? AND company_id=?)",Boolean.class,id,actor.companyId())))notFound();
    }
    private static void window(int page,int size){if(page<0||page>1000||size<1||size>100)bad("Use página de 0 a 1000 e tamanho de 1 a 100.");}
    private static void stage(String value){if(!STAGES.contains(Objects.toString(value,"")))bad("Escolha uma etapa válida para a candidatura.");}
    private static void bad(String message){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
    private static void notFound(){throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Registro não encontrado.");}
    private static void conflict(){throw new ResponseStatusException(HttpStatus.CONFLICT,"A candidatura mudou. Atualize os dados antes de tentar novamente.");}
    private static void version(Long value,UUID key){if(value==null||value<0||key==null)bad("Informe expectedRevision e requestKey para confirmar a operação.");}
    private Current current(Actor actor,long id,boolean lock){
        var rows=jdbc.query("SELECT a.status,a.revision FROM company_applications a JOIN company_listings l ON l.id=a.listing_id WHERE a.id=? AND l.company_id=?"+(lock?" FOR UPDATE OF a":""),
                (rs,n)->new Current(rs.getString(1),rs.getLong(2)),id,actor.companyId());
        if(rows.isEmpty())notFound();return rows.getFirst();
    }
    @Transactional(transactionManager="empresasTransactionManager",readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Page<Application> applications(String email,Long listingId,String stage,int page,int size){
        window(page,size);var f=filter(actor(email),listingId,stage);
        long count=jdbc.queryForObject("SELECT count(*)"+f.sql(),Long.class,f.args().toArray());
        var args=new ArrayList<>(f.args());args.add(size);args.add((long)page*size);
        var items=jdbc.query("SELECT a.id,a.listing_id,l.title,l.kind,a.candidate_name,a.candidate_email,a.candidate_phone,a.message,a.status,a.revision,a.created_at,a.updated_at"+f.sql()+" ORDER BY a.created_at DESC,a.id DESC LIMIT ? OFFSET ?",
            (rs,n)->new Application(rs.getLong(1),rs.getLong(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6),Objects.toString(rs.getString(7),""),Objects.toString(rs.getString(8),""),rs.getString(9),rs.getLong(10),rs.getTimestamp(11).toInstant(),rs.getTimestamp(12).toInstant()),args.toArray());
        return new Page<>(items,page,size,count,(long)(page+1)*size<count);
    }
    @Transactional(transactionManager="empresasTransactionManager",readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Application application(String email,long id){
        var who=actor(email);current(who,id,false);
        return jdbc.queryForObject("SELECT a.id,a.listing_id,l.title,l.kind,a.candidate_name,a.candidate_email,a.candidate_phone,a.message,a.status,a.revision,a.created_at,a.updated_at FROM company_applications a JOIN company_listings l ON l.id=a.listing_id WHERE a.id=? AND l.company_id=?",
            (rs,n)->new Application(rs.getLong(1),rs.getLong(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6),Objects.toString(rs.getString(7),""),Objects.toString(rs.getString(8),""),rs.getString(9),rs.getLong(10),rs.getTimestamp(11).toInstant(),rs.getTimestamp(12).toInstant()),id,who.companyId());
    }
    @Transactional(transactionManager="empresasTransactionManager",readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Indicators indicators(String email,Long listingId,String stage){
        var f=filter(actor(email),listingId,stage);var counts=new LinkedHashMap<String,Long>();STAGES.forEach(s->counts.put(s,0L));
        jdbc.query("SELECT a.status,count(*)"+f.sql()+" GROUP BY a.status",rs->{counts.put(rs.getString(1),rs.getLong(2));},f.args().toArray());
        long total=counts.values().stream().mapToLong(Long::longValue).sum();
        return new Indicators(total,counts,counts.get("RECEIVED"),counts.get("IN_REVIEW")+counts.get("INTERVIEW")+counts.get("CONTACTED"),counts.get("ACCEPTED"),"Selecionado não significa contratação confirmada. A etapa Entrevista não comprova sua realização. CONTACTED preserva os contatos legados.");
    }
    private String hash(String... parts){
        try{var digest=MessageDigest.getInstance("SHA-256");for(var part:parts){byte[] bytes=Objects.toString(part,"").getBytes(StandardCharsets.UTF_8);digest.update(java.nio.ByteBuffer.allocate(4).putInt(bytes.length).array());digest.update(bytes);}return HexFormat.of().formatHex(digest.digest());}
        catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }
    private Mutation replay(Actor actor,long id,UUID key,String hash){
        if(key==null)return null;
        var rows=jdbc.queryForList("SELECT status,revision,note_id,request_hash,actor_user_id FROM company_recruitment_events WHERE application_id=? AND request_key=?",id,key);
        if(rows.isEmpty())return null;var old=rows.getFirst();
        if(!hash.equals(old.get("request_hash"))||old.get("actor_user_id")==null||((Number)old.get("actor_user_id")).longValue()!=actor.userId())conflict();
        return new Mutation((String)old.get("status"),((Number)old.get("revision")).longValue(),(UUID)old.get("note_id"),true);
    }
    private void event(Actor actor,long id,long revision,String type,String previous,String status,UUID key,String hash,UUID note){
        jdbc.update("INSERT INTO company_recruitment_events(application_id,revision,event_type,previous_status,status,actor_user_id,request_key,request_hash,note_id) VALUES (?,?,?,?,?,?,?,?,?)",id,revision,type,previous,status,actor.userId(),key,hash,note);
    }
    private long update(Actor actor,long id,Current old,String status){
        if(jdbc.update("UPDATE company_applications a SET status=?,revision=revision+1,updated_at=CURRENT_TIMESTAMP FROM company_listings l WHERE a.listing_id=l.id AND a.id=? AND l.company_id=? AND a.revision=?",status,id,actor.companyId(),old.revision())!=1)conflict();
        return old.revision()+1;
    }
    @Transactional("empresasTransactionManager")
    public Mutation change(String email,long id,Change input){return changeInternal(email,id,input,false);}
    // Compatibility endpoint retains explicit last-write intent; the new endpoint always requires optimistic concurrency.
    @Transactional("empresasTransactionManager")
    public Mutation legacyChange(String email,long id,Change input){return changeInternal(email,id,input,true);}
    private Mutation changeInternal(String email,long id,Change input,boolean legacy){
        var who=actor(email);if(input==null)bad("Informe a etapa.");stage(input.status());
        if(!legacy||input.expectedRevision()!=null||input.requestKey()!=null)version(input.expectedRevision(),input.requestKey());
        var old=current(who,id,true);
        String hash=hash("STAGE",input.status(),Objects.toString(input.expectedRevision(),""),Boolean.toString(Boolean.TRUE.equals(input.reopen())));
        var prior=replay(who,id,input.requestKey(),hash);if(prior!=null)return prior;
        if(input.expectedRevision()!=null&&input.expectedRevision()!=old.revision())conflict();
        if("CLOSED".equals(old.status())&&!"CLOSED".equals(input.status())&&!Boolean.TRUE.equals(input.reopen()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Confirme reopen=true para reabrir o atendimento.");
        // A versioned no-op is recorded so its requestKey remains idempotent after later changes.
        if(legacy&&input.requestKey()==null&&old.status().equals(input.status()))return new Mutation(old.status(),old.revision(),null,false);
        long revision=update(who,id,old,input.status());event(who,id,revision,"STAGE_CHANGED",old.status(),input.status(),input.requestKey(),hash,null);
        if(domainEvents!=null&&!old.status().equals(input.status())) domainEvents.append(jdbc,"EMPRESAS","application.stage_changed.v1","application",Long.toString(id),"application-stage:"+id+":"+revision,Map.of("applicationId",id,"revision",revision,"previousStatus",old.status(),"status",input.status()));
        return new Mutation(input.status(),revision,null,false);
    }
    @Transactional("empresasTransactionManager")
    public Mutation addNote(String email,long id,Note input){
        var who=actor(email);if(input==null||input.body()==null||input.body().isBlank()||input.body().length()>2000||input.body().indexOf(0)>=0)bad("Escreva uma observação de 1 a 2.000 caracteres, sem dados sensíveis desnecessários.");
        version(input.expectedRevision(),input.requestKey());var old=current(who,id,true);
        String body=input.body().trim(),hash=hash("NOTE",body,input.expectedRevision().toString());
        var prior=replay(who,id,input.requestKey(),hash);if(prior!=null)return prior;
        if(input.expectedRevision()!=old.revision())conflict();
        UUID note=UUID.randomUUID();
        jdbc.update("INSERT INTO company_recruitment_notes(id,application_id,author_user_id,body,expires_at) VALUES (?,?,?,?,CURRENT_TIMESTAMP+make_interval(days=>?))",note,id,who.userId(),body,notesDays);
        long revision=update(who,id,old,old.status());event(who,id,revision,"NOTE_ADDED",old.status(),old.status(),input.requestKey(),hash,note);
        return new Mutation(old.status(),revision,note,false);
    }
    @Transactional("empresasTransactionManager")
    public Mutation removeNote(String email,long id,UUID note,Remove input){
        var who=actor(email);if(input==null)bad("Informe a versão e a identificação do pedido.");version(input.expectedRevision(),input.requestKey());var old=current(who,id,true);
        String hash=hash("REMOVE",note.toString(),input.expectedRevision().toString());var prior=replay(who,id,input.requestKey(),hash);if(prior!=null)return prior;
        if(input.expectedRevision()!=old.revision())conflict();
        if(jdbc.update("UPDATE company_recruitment_notes SET body=NULL,deleted_at=CURRENT_TIMESTAMP WHERE id=? AND application_id=? AND deleted_at IS NULL",note,id)!=1)notFound();
        long revision=update(who,id,old,old.status());event(who,id,revision,"NOTE_REMOVED",old.status(),old.status(),input.requestKey(),hash,note);
        return new Mutation(old.status(),revision,note,false);
    }
    @Transactional(transactionManager="empresasTransactionManager",readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Page<Event> history(String email,long id,int page,int size){
        window(page,size);var who=actor(email);current(who,id,false);
        long count=jdbc.queryForObject("SELECT count(*) FROM company_recruitment_events WHERE application_id=?",Long.class,id);
        var items=jdbc.query("SELECT id,revision,event_type,previous_status,status,actor_user_id,note_id,created_at FROM company_recruitment_events WHERE application_id=? ORDER BY id DESC LIMIT ? OFFSET ?",(rs,n)->new Event(rs.getLong(1),rs.getLong(2),rs.getString(3),rs.getString(4),rs.getString(5),(Long)rs.getObject(6),(UUID)rs.getObject(7),rs.getTimestamp(8).toInstant()),id,size,(long)page*size);
        return new Page<>(items,page,size,count,(long)(page+1)*size<count);
    }
    @Transactional(transactionManager="empresasTransactionManager",readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Page<InternalNote> notes(String email,long id,int page,int size){
        window(page,size);var who=actor(email);current(who,id,false);
        String where=" FROM company_recruitment_notes WHERE application_id=? AND deleted_at IS NULL AND expires_at>CURRENT_TIMESTAMP";
        long count=jdbc.queryForObject("SELECT count(*)"+where,Long.class,id);
        var items=jdbc.query("SELECT id,body,author_user_id,created_at,expires_at"+where+" ORDER BY created_at DESC,id DESC LIMIT ? OFFSET ?",(rs,n)->new InternalNote((UUID)rs.getObject(1),rs.getString(2),(Long)rs.getObject(3),rs.getTimestamp(4).toInstant(),rs.getTimestamp(5).toInstant()),id,size,(long)page*size);
        return new Page<>(items,page,size,count,(long)(page+1)*size<count);
    }
}
