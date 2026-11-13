package com.trampolim.api.career;

import com.trampolim.api.radar.SkillCatalog;
import com.trampolim.api.security.BoundedRateLimiter;
import com.trampolim.api.security.PlatformAccess;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Private mentoring workflow isolated to the Oportunidades database and account context. */
@Service
public class CareerMentorshipService {
    private final JdbcTemplate db;
    private final SkillCatalog users;
    private final PlatformAccess admins;
    private final BoundedRateLimiter limiter = new BoundedRateLimiter();

    public CareerMentorshipService(@Qualifier("jdbcTemplate") JdbcTemplate db, SkillCatalog users, PlatformAccess admins) {
        this.db = db; this.users = users; this.admins = admins;
    }

    public record MentorInput(String displayName, String biography, String experience, List<String> specialties, boolean adultConfirmed, boolean publicProfileConsent) {}
    public record SlotInput(String startsAt, String endsAt, String modality, String publicArea) {}
    public record BookingInput(long availabilityId, String topic, String message, boolean adultConfirmed) {}
    public record SessionDetails(String meetingDetails) {}
    public record RatingInput(int rating, String comment) {}
    public record ReportInput(UUID mentorId, UUID sessionId, String reason, String details) {}
    public record MentorDecision(String status, String note) {}
    public record ReportDecision(String status, String decision, String note, boolean suspendMentor) {}

    public Map<String,Object> me(String email) {
        long id=users.user(email);
        var rows=db.queryForList("SELECT public_id,display_name,biography,experience_summary,status,review_note,created_at,updated_at FROM career_mentor_profiles WHERE user_id=?",id);
        Map<String,Object> profile=rows.isEmpty()?Map.of("status","NONE"):rows.get(0);
        if(!rows.isEmpty())profile.put("specialties",specialties(id));
        return Map.of("mentor",profile,"isPlatformAdmin",admins.isSuperAdmin(email),"adultOnly",true,
                "notice","Mentoria individual disponível somente a pessoas que confirmam ter 18 anos ou mais. A declaração não é verificação documental.");
    }

    @Transactional("oportunidadesTransactionManager")
    public Map<String,Object> saveMentor(String email,MentorInput input) {
        long uid=users.user(email);
        if(input==null||!input.adultConfirmed())bad("Confirme que você tem 18 anos ou mais para se candidatar como mentor.");if(!input.publicProfileConsent())bad("Autorize a exibição do perfil profissional público caso ele seja aprovado.");
        String name=text(input.displayName(),80),bio=text(input.biography(),1200),experience=text(input.experience(),1200);
        if(name.isBlank()||experience.isBlank())bad("Informe nome público e experiência profissional.");
        if(containsContact(name)||containsContact(bio)||containsContact(experience))bad("Não inclua telefone, e-mail ou endereço nas informações públicas do perfil.");
        var tags=cleanTags(input.specialties());
        var existing=db.queryForList("SELECT status FROM career_mentor_profiles WHERE user_id=? FOR UPDATE",uid);
        if(!existing.isEmpty()&&"SUSPENDED".equals(existing.get(0).get("status")))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Este cadastro está suspenso e aguarda revisão administrativa.");
        if(!existing.isEmpty()&&"APPROVED".equals(existing.get(0).get("status")))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Seu perfil já está aprovado. Solicite à equipe uma revisão antes de alterar os dados públicos.");
        UUID publicId=existing.isEmpty()?UUID.randomUUID():db.queryForObject("SELECT public_id FROM career_mentor_profiles WHERE user_id=?",UUID.class,uid);
        db.update("INSERT INTO career_mentor_profiles(user_id,public_id,display_name,biography,experience_summary,status,review_note,reviewed_by,reviewed_at,adult_confirmed_at,public_profile_consent_at) VALUES (?,?,?,?,?,'PENDING','',NULL,NULL,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP) ON CONFLICT(user_id) DO UPDATE SET display_name=EXCLUDED.display_name,biography=EXCLUDED.biography,experience_summary=EXCLUDED.experience_summary,status='PENDING',review_note='',reviewed_by=NULL,reviewed_at=NULL,adult_confirmed_at=CURRENT_TIMESTAMP,public_profile_consent_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP",uid,publicId,name,bio,experience);
        db.update("DELETE FROM career_mentor_specialties WHERE mentor_user_id=?",uid);
        for(String tag:tags)db.update("INSERT INTO career_mentor_specialties(mentor_user_id,specialty) VALUES (?,?)",uid,tag);
        db.update("UPDATE career_mentor_availability SET status='CLOSED' WHERE mentor_user_id=? AND status='OPEN'",uid);
        return me(email);
    }

    @Transactional("oportunidadesTransactionManager")
    public void withdrawMentor(String email) {
        long uid=users.user(email);var profiles=db.queryForList("SELECT status FROM career_mentor_profiles WHERE user_id=? FOR UPDATE",uid);if(profiles.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Perfil de mentor não encontrado.");if("WITHDRAWN".equals(profiles.getFirst().get("status")))return;
        db.update("UPDATE career_mentor_profiles SET status='WITHDRAWN',review_note='',reviewed_by=NULL,reviewed_at=NULL,updated_at=CURRENT_TIMESTAMP WHERE user_id=?",uid);
        db.update("UPDATE career_mentor_availability SET status='CLOSED' WHERE mentor_user_id=? AND status='OPEN'",uid);
        var sessions=db.queryForList("UPDATE career_mentoring_sessions s SET status='CANCELLED',updated_at=CURRENT_TIMESTAMP FROM career_mentor_availability a WHERE a.id=s.availability_id AND s.mentor_user_id=? AND s.status IN ('REQUESTED','CONFIRMED') AND a.starts_at>CURRENT_TIMESTAMP RETURNING s.id,s.availability_id",uid);
        for(var row:sessions){db.update("UPDATE career_mentor_availability SET status='CLOSED' WHERE id=?",row.get("availability_id"));event((UUID)row.get("id"),uid,"CANCELLED","Encontro cancelado após retirada do perfil público do mentor.");}
    }

    public Map<String,Object> mentors(String specialty,UUID before,int limit) {
        if(limit<1||limit>50)bad("O limite deve estar entre 1 e 50.");
        String tag=text(specialty,80).toLowerCase(Locale.ROOT);
        var rows=db.queryForList("SELECT p.public_id,p.display_name,p.biography,p.experience_summary,p.user_id,(SELECT count(*) FROM career_mentor_availability a WHERE a.mentor_user_id=p.user_id AND a.status='OPEN' AND a.starts_at>CURRENT_TIMESTAMP) AS \"openSlots\" FROM career_mentor_profiles p WHERE p.status='APPROVED' AND (?='' OR EXISTS(SELECT 1 FROM career_mentor_specialties s WHERE s.mentor_user_id=p.user_id AND LOWER(s.specialty)=?)) AND (?::uuid IS NULL OR p.public_id<?::uuid) ORDER BY p.public_id DESC LIMIT ?",tag,tag,before,before,limit+1);
        boolean more=rows.size()>limit;var page=rows.stream().limit(limit).toList();var items=new ArrayList<Map<String,Object>>();
        if(!page.isEmpty()){var owners=page.stream().map(row->((Number)row.get("user_id")).longValue()).toList();String placeholders=String.join(",",Collections.nCopies(owners.size(),"?"));var tagRows=db.queryForList("SELECT mentor_user_id,specialty FROM career_mentor_specialties WHERE mentor_user_id IN ("+placeholders+") ORDER BY specialty",owners.toArray());var tagsByOwner=new HashMap<Long,List<String>>();for(var tagRow:tagRows)tagsByOwner.computeIfAbsent(((Number)tagRow.get("mentor_user_id")).longValue(),ignored->new ArrayList<>()).add((String)tagRow.get("specialty"));for(var row:page){long owner=((Number)row.remove("user_id")).longValue();row.put("specialties",tagsByOwner.getOrDefault(owner,List.of()));items.add(row);}}
        return Map.of("items",items,"nextCursor",more?items.get(items.size()-1).get("public_id"):"");
    }

    public List<Map<String,Object>> publicSlots(UUID mentorId) {
        return db.queryForList("SELECT a.id,a.starts_at AS \"startsAt\",a.ends_at AS \"endsAt\",a.modality,a.public_area AS \"publicArea\" FROM career_mentor_availability a JOIN career_mentor_profiles p ON p.user_id=a.mentor_user_id WHERE p.public_id=? AND p.status='APPROVED' AND a.status='OPEN' AND a.starts_at>CURRENT_TIMESTAMP ORDER BY a.starts_at LIMIT 50",mentorId);
    }

    @Transactional("oportunidadesTransactionManager")
    public Map<String,Object> addSlot(String email,SlotInput input) {
        long uid=users.user(email);var mentorRow=db.queryForList("SELECT status FROM career_mentor_profiles WHERE user_id=? FOR UPDATE",uid);if(mentorRow.isEmpty()||!"APPROVED".equals(mentorRow.getFirst().get("status")))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"O perfil de mentor precisa estar aprovado para oferecer horários.");
        if(input==null)bad("Informe horário e modalidade.");
        Instant start=parse(input.startsAt()),end=parse(input.endsAt()),now=Instant.now();
        if(start.isBefore(now.plus(Duration.ofHours(24)))||start.isAfter(now.plus(Duration.ofDays(90)))||!end.isAfter(start)||Duration.between(start,end).toMinutes()<30||Duration.between(start,end).toMinutes()>120)bad("Ofereça horários de 30 a 120 minutos, entre 24 horas e 90 dias a partir de agora.");
        String modality=Objects.toString(input.modality(),"").toUpperCase(Locale.ROOT);if(!Set.of("ONLINE","IN_PERSON").contains(modality))bad("Escolha online ou presencial.");
        String area=text(input.publicArea(),120);if(modality.equals("IN_PERSON")&&area.isBlank())bad("Informe município ou localidade geral, sem endereço residencial.");if(containsPrivateLocation(area))bad("Informe apenas município ou região geral, sem rua, número ou endereço.");if(modality.equals("ONLINE"))area="";
        if(db.queryForObject("SELECT count(*) FROM career_mentor_availability WHERE mentor_user_id=? AND status IN ('OPEN','BOOKED') AND starts_at<? AND ends_at>?",Integer.class,uid,java.sql.Timestamp.from(end),java.sql.Timestamp.from(start))>0)bad("Este horário se sobrepõe a outro horário já oferecido.");
        Long id=db.queryForObject("INSERT INTO career_mentor_availability(mentor_user_id,starts_at,ends_at,modality,public_area) VALUES (?,?,?,?,?) RETURNING id",Long.class,uid,java.sql.Timestamp.from(start),java.sql.Timestamp.from(end),modality,area);
        return db.queryForMap("SELECT id,starts_at AS \"startsAt\",ends_at AS \"endsAt\",modality,public_area AS \"publicArea\",status FROM career_mentor_availability WHERE id=?",id);
    }

    public List<Map<String,Object>> mySlots(String email) {
        long uid=users.user(email);return db.queryForList("SELECT id,starts_at AS \"startsAt\",ends_at AS \"endsAt\",modality,public_area AS \"publicArea\",status FROM career_mentor_availability WHERE mentor_user_id=? AND starts_at>CURRENT_TIMESTAMP ORDER BY starts_at LIMIT 100",uid);
    }

    @Transactional("oportunidadesTransactionManager")
    public void closeSlot(String email,long slotId) {
        long uid=users.user(email);if(db.update("UPDATE career_mentor_availability SET status='CLOSED' WHERE id=? AND mentor_user_id=? AND status='OPEN' AND starts_at>CURRENT_TIMESTAMP",slotId,uid)==0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Horário inexistente, reservado ou já encerrado.");
    }

    @Transactional("oportunidadesTransactionManager")
    public Map<String,Object> book(String email,BookingInput input) {
        long mentee=users.user(email);if(input==null||!input.adultConfirmed())bad("Confirme que você tem 18 anos ou mais para solicitar uma mentoria individual.");if(input.availabilityId()<1)bad("Escolha um horário disponível.");db.queryForObject("SELECT id FROM app_users WHERE id=? FOR UPDATE",Long.class,mentee);
        String topic=text(input.topic(),120),message=text(input.message(),1000);if(topic.isBlank())bad("Informe o assunto que deseja conversar.");
        if(!limiter.allow(Long.toString(mentee),3,Duration.ofMinutes(1)))throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Aguarde antes de enviar outra solicitação.");
        var slot=db.queryForList("SELECT id,mentor_user_id,starts_at,ends_at,modality,public_area,status FROM career_mentor_availability WHERE id=? FOR UPDATE",input.availabilityId());if(slot.isEmpty()||!"OPEN".equals(slot.getFirst().get("status")))throw new ResponseStatusException(HttpStatus.CONFLICT,"Este horário não está mais disponível.");
        var selected=slot.getFirst();long mentor=((Number)selected.get("mentor_user_id")).longValue();if(mentor==mentee)bad("Escolha o horário de outra pessoa.");if(!"APPROVED".equals(db.queryForObject("SELECT status FROM career_mentor_profiles WHERE user_id=?",String.class,mentor)))throw new ResponseStatusException(HttpStatus.CONFLICT,"Este mentor não está disponível.");
        if(db.queryForObject("SELECT count(*) FROM career_mentoring_sessions WHERE mentee_user_id=? AND created_at>=CURRENT_DATE",Integer.class,mentee)>=5)throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Limite diário de cinco solicitações atingido.");
        UUID id=UUID.randomUUID();db.update("INSERT INTO career_mentoring_sessions(id,availability_id,mentor_user_id,mentee_user_id,topic,request_message,mentee_adult_confirmed_at) VALUES (?,?,?,?,?,?,CURRENT_TIMESTAMP)",id,input.availabilityId(),mentor,mentee,topic,message);
        db.update("UPDATE career_mentor_availability SET status='BOOKED' WHERE id=? AND status='OPEN'",input.availabilityId());event(id,mentee,"REQUESTED","");return ownSession(mentee,id);
    }

    public List<Map<String,Object>> sessions(String email) {
        long uid=users.user(email);var rows=db.queryForList("SELECT id FROM career_mentoring_sessions WHERE mentee_user_id=? OR mentor_user_id=? ORDER BY created_at DESC LIMIT 100",uid,uid);var result=new ArrayList<Map<String,Object>>();for(var row:rows)result.add(ownSession(uid,(UUID)row.get("id")));return result;
    }

    public Map<String,Object> ownSession(long uid,UUID id) {
        var rows=db.queryForList("SELECT s.id,s.mentor_user_id,s.mentee_user_id,s.topic,s.request_message,s.status,s.meeting_details,a.starts_at AS \"startsAt\",a.ends_at AS \"endsAt\",a.modality,a.public_area AS \"publicArea\",p.public_id AS \"mentorPublicId\",p.display_name AS \"mentorName\",CASE WHEN s.mentee_user_id=? THEN 'MENTEE' ELSE 'MENTOR' END AS role,CASE WHEN s.mentee_user_id=? THEN p.display_name ELSE m.full_name END AS \"otherParticipantName\",s.created_at AS \"createdAt\" FROM career_mentoring_sessions s JOIN career_mentor_availability a ON a.id=s.availability_id JOIN career_mentor_profiles p ON p.user_id=s.mentor_user_id JOIN app_users m ON m.id=s.mentee_user_id WHERE s.id=? AND (s.mentee_user_id=? OR s.mentor_user_id=?)",uid,uid,id,uid,uid);
        if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Sessão não encontrada.");var row=rows.getFirst();long mentee=((Number)row.get("mentee_user_id")).longValue();row.remove("mentor_user_id");row.remove("mentee_user_id");var rating=uid==mentee?db.queryForList("SELECT rating,comment,created_at AS \"createdAt\" FROM career_mentoring_ratings WHERE session_id=? AND mentee_user_id=?",id,mentee):List.<Map<String,Object>>of();row.put("myRating",rating.isEmpty()?null:rating.getFirst());return row;
    }

    @Transactional("oportunidadesTransactionManager")
    public Map<String,Object> confirm(String email,UUID id,boolean confirm) {
        long uid=users.user(email);var session=lockSession(uid,id);if(!session.get("status").equals("REQUESTED")||((Number)session.get("mentor_user_id")).longValue()!=uid)throw new ResponseStatusException(HttpStatus.CONFLICT,"Somente o mentor pode responder a uma solicitação pendente.");
        long slot=((Number)session.get("availability_id")).longValue();boolean future=Boolean.TRUE.equals(db.queryForObject("SELECT starts_at>CURRENT_TIMESTAMP FROM career_mentor_availability WHERE id=?",Boolean.class,slot));if(!future)throw new ResponseStatusException(HttpStatus.CONFLICT,"O horário já passou.");String status=confirm?"CONFIRMED":"DECLINED";db.update("UPDATE career_mentoring_sessions SET status=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",status,id);if(!confirm)db.update("UPDATE career_mentor_availability SET status='OPEN' WHERE id=? AND status='BOOKED'",slot);event(id,uid,status,"");return ownSession(uid,id);
    }

    @Transactional("oportunidadesTransactionManager")
    public Map<String,Object> cancel(String email,UUID id) {
        long uid=users.user(email);var session=lockSession(uid,id);if(Set.of("CANCELLED","DECLINED").contains(session.get("status"))||"COMPLETED".equals(session.get("status")))throw new ResponseStatusException(HttpStatus.CONFLICT,"Esta sessão não pode mais ser cancelada.");long slot=((Number)session.get("availability_id")).longValue();String status="CANCELLED";db.update("UPDATE career_mentoring_sessions SET status=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",status,id);db.update("UPDATE career_mentor_availability SET status=CASE WHEN starts_at>CURRENT_TIMESTAMP THEN 'OPEN' ELSE 'CLOSED' END WHERE id=? AND status='BOOKED'",slot);event(id,uid,status,"");return ownSession(uid,id);
    }

    @Transactional("oportunidadesTransactionManager")
    public Map<String,Object> complete(String email,UUID id) {
        long uid=users.user(email);var session=lockSession(uid,id);if(!"CONFIRMED".equals(session.get("status")))throw new ResponseStatusException(HttpStatus.CONFLICT,"Somente uma sessão confirmada pode ser concluída.");boolean ended=Boolean.TRUE.equals(db.queryForObject("SELECT ends_at<=CURRENT_TIMESTAMP FROM career_mentor_availability WHERE id=?",Boolean.class,session.get("availability_id")));if(!ended)throw new ResponseStatusException(HttpStatus.CONFLICT,"A sessão ainda não terminou.");db.update("UPDATE career_mentoring_sessions SET status='COMPLETED',completed_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP WHERE id=?",id);event(id,uid,"COMPLETED","Participante registrou a conclusão; encontro não foi gravado.");return ownSession(uid,id);
    }

    @Transactional("oportunidadesTransactionManager")
    public Map<String,Object> meetingDetails(String email,UUID id,SessionDetails input) {
        long uid=users.user(email);if(input==null)bad("Informe os detalhes ou link do encontro.");var session=lockSession(uid,id);if(((Number)session.get("mentor_user_id")).longValue()!=uid||!"CONFIRMED".equals(session.get("status")))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Somente o mentor pode incluir detalhes em uma sessão confirmada.");String details=text(input.meetingDetails(),500);if(containsContact(details)||containsPrivateLocation(details))bad("Não inclua telefone, e-mail ou endereço pessoal. Compartilhe somente informações necessárias ao encontro.");if(details.toLowerCase(Locale.ROOT).contains("http")&&!https(details))bad("Links precisam usar HTTPS válido.");db.update("UPDATE career_mentoring_sessions SET meeting_details=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",details,id);event(id,uid,"DETAILS_UPDATED","O mentor atualizou detalhes privados do encontro.");return ownSession(uid,id);
    }

    @Transactional("oportunidadesTransactionManager")
    public Map<String,Object> rate(String email,UUID id,RatingInput input) {
        long uid=users.user(email);if(input==null||input.rating()<1||input.rating()>5)bad("A nota deve ser de 1 a 5.");var session=lockSession(uid,id);if(((Number)session.get("mentee_user_id")).longValue()!=uid||!"COMPLETED".equals(session.get("status")))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Somente quem solicitou pode avaliar uma sessão concluída.");db.update("INSERT INTO career_mentoring_ratings(session_id,mentee_user_id,rating,comment) VALUES (?,?,?,?) ON CONFLICT(session_id) DO UPDATE SET rating=EXCLUDED.rating,comment=EXCLUDED.comment,created_at=CURRENT_TIMESTAMP",id,uid,input.rating(),text(input.comment(),1000));return ownSession(uid,id);
    }

    @Transactional("oportunidadesTransactionManager")
    public Map<String,Object> report(String email,ReportInput input) {
        long uid=users.user(email);if(input==null||input.mentorId()==null||!Set.of("SAFETY","HARASSMENT","MISCONDUCT","PRIVACY","OTHER").contains(Objects.toString(input.reason(),"")))bad("Selecione o mentor e o motivo da denúncia.");if(!limiter.allow("report-"+uid,2,Duration.ofHours(1)))throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Aguarde antes de enviar outra denúncia.");Long target=db.query("SELECT user_id FROM career_mentor_profiles WHERE public_id=?",(rs,n)->rs.getLong(1),input.mentorId()).stream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));if(target==uid)bad("Não é possível denunciar a própria conta.");if(input.sessionId()!=null){var session=lockSession(uid,input.sessionId());if(((Number)session.get("mentor_user_id")).longValue()!=target)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"A sessão não corresponde ao mentor escolhido.");}
        UUID id=UUID.randomUUID();db.update("INSERT INTO career_mentoring_reports(id,reporter_user_id,target_user_id,session_id,reason,details) VALUES (?,?,?,?,?,?)",id,uid,target,input.sessionId(),input.reason(),text(input.details(),1500));return Map.of("id",id,"status","PENDING","notice","A denúncia é privada e será revisada pela equipe autorizada.");
    }

    public List<Map<String,Object>> history(String email,UUID id) {
        long uid=users.user(email);lockSession(uid,id);return db.queryForList("SELECT event_type AS \"eventType\",details,created_at AS \"createdAt\" FROM career_mentoring_events WHERE session_id=? ORDER BY created_at,id",id);
    }

    public List<Map<String,Object>> adminMentors(String email) { requireAdmin(email);return db.queryForList("SELECT p.public_id AS id,p.display_name AS \"displayName\",p.biography,p.experience_summary AS \"experienceSummary\",p.status,p.review_note AS \"reviewNote\",p.created_at AS \"createdAt\" FROM career_mentor_profiles p WHERE p.status IN ('PENDING','REJECTED','SUSPENDED') ORDER BY p.created_at LIMIT 100"); }

    @Transactional("oportunidadesTransactionManager")
    public Map<String,Object> reviewMentor(String email,UUID publicId,MentorDecision input) {
        long reviewer=adminId(email);if(input==null||!Set.of("APPROVED","REJECTED","SUSPENDED").contains(Objects.toString(input.status(),""))||text(input.note(),20).length()<20)bad("Informe uma decisão e registre a justificativa da revisão com pelo menos vinte caracteres.");
        Long owner=db.query("SELECT user_id FROM career_mentor_profiles WHERE public_id=? FOR UPDATE",(rs,n)->rs.getLong(1),publicId).stream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        if(owner==reviewer)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"A revisão precisa ser independente do mentor.");
        db.update("UPDATE career_mentor_profiles SET status=?,review_note=?,reviewed_by=?,reviewed_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP WHERE public_id=?",input.status(),text(input.note(),500),reviewer,publicId);
        if(input.status().equals("SUSPENDED"))suspendUpcoming(owner,reviewer,"Suspensão temporária após revisão administrativa.");else if(input.status().equals("REJECTED"))db.update("UPDATE career_mentor_availability SET status='CLOSED' WHERE mentor_user_id=? AND status='OPEN'",owner);
        return db.queryForMap("SELECT public_id AS id,display_name AS \"displayName\",status,review_note AS \"reviewNote\" FROM career_mentor_profiles WHERE public_id=?",publicId);
    }

    public List<Map<String,Object>> adminReports(String email) {requireAdmin(email);return db.queryForList("SELECT r.id,r.reason,r.details,r.status,r.created_at AS \"createdAt\",p.public_id AS \"mentorId\",p.display_name AS \"mentorName\",r.session_id AS \"sessionId\",r.review_note AS \"reviewNote\" FROM career_mentoring_reports r LEFT JOIN career_mentor_profiles p ON p.user_id=r.target_user_id WHERE r.status IN ('PENDING','REVIEWING') ORDER BY r.created_at LIMIT 100");}

    @Transactional("oportunidadesTransactionManager")
    public Map<String,Object> reviewReport(String email,UUID reportId,ReportDecision input) {
        long reviewer=adminId(email);if(input==null||!Set.of("RESOLVED","DISMISSED").contains(Objects.toString(input.status(),""))||!Set.of("ACTION_TAKEN","NO_ACTION","NEEDS_FOLLOWUP").contains(Objects.toString(input.decision(),""))||text(input.note(),20).length()<20)bad("Registre decisão e justificativa de pelo menos vinte caracteres.");
        var reports=db.queryForList("SELECT target_user_id,status FROM career_mentoring_reports WHERE id=? FOR UPDATE",reportId);if(reports.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND);if(!Set.of("PENDING","REVIEWING").contains(reports.getFirst().get("status")))throw new ResponseStatusException(HttpStatus.CONFLICT,"Esta denúncia já foi encerrada.");
        Object target=reports.getFirst().get("target_user_id");if(input.suspendMentor()){if(!"ACTION_TAKEN".equals(input.decision())||target==null)bad("Suspensão exige ação moderadora e um mentor identificado.");suspendUpcoming(((Number)target).longValue(),reviewer,"Suspensão temporária após revisão de denúncia.");}
        db.update("UPDATE career_mentoring_reports SET status=?,decision=?,review_note=?,reviewed_by=?,reviewed_at=CURRENT_TIMESTAMP WHERE id=?",input.status(),input.decision(),text(input.note(),1000),reviewer,reportId);return Map.of("id",reportId,"status",input.status(),"decision",input.decision());
    }

    public Map<String,Object> metrics(String email) {
        requireAdmin(email);return Map.of("approvedMentors",db.queryForObject("SELECT count(*) FROM career_mentor_profiles WHERE status='APPROVED'",Long.class),"openSlots",db.queryForObject("SELECT count(*) FROM career_mentor_availability WHERE status='OPEN' AND starts_at>CURRENT_TIMESTAMP",Long.class),"sessionsByStatus",db.queryForMap("SELECT count(*) FILTER(WHERE status='REQUESTED') AS requested,count(*) FILTER(WHERE status='CONFIRMED') AS confirmed,count(*) FILTER(WHERE status='COMPLETED') AS completed,count(*) FILTER(WHERE status IN ('DECLINED','CANCELLED')) AS closed FROM career_mentoring_sessions"),"reportsOpen",db.queryForObject("SELECT count(*) FROM career_mentoring_reports WHERE status IN ('PENDING','REVIEWING')",Long.class),"experienceRating",db.queryForMap("SELECT count(*) AS responses,round(avg(rating)::numeric,1) AS average FROM career_mentoring_ratings"),"notice","Totais reais do módulo; não são estimativas de empregabilidade nem resultados de contratação.");
    }

    @Scheduled(fixedDelayString="${career.mentoring.cleanup-ms:60000}")
    @Transactional("oportunidadesTransactionManager")
    public void expireUnconfirmedSessions() {
        try {var expired=db.queryForList("WITH due AS (SELECT s.id FROM career_mentoring_sessions s JOIN career_mentor_availability a ON a.id=s.availability_id WHERE s.status='REQUESTED' AND (a.starts_at<=CURRENT_TIMESTAMP OR (s.created_at<=a.starts_at-INTERVAL '24 hours' AND CURRENT_TIMESTAMP>=a.starts_at-INTERVAL '24 hours')) ORDER BY s.created_at LIMIT 100 FOR UPDATE OF s SKIP LOCKED) UPDATE career_mentoring_sessions s SET status='CANCELLED',updated_at=CURRENT_TIMESTAMP FROM due,career_mentor_availability a WHERE s.id=due.id AND a.id=s.availability_id RETURNING s.id,s.availability_id");for(var row:expired){UUID id=(UUID)row.get("id");db.update("UPDATE career_mentor_availability SET status=CASE WHEN starts_at>CURRENT_TIMESTAMP THEN 'OPEN' ELSE 'CLOSED' END WHERE id=? AND status='BOOKED'",row.get("availability_id"));event(id,null,"CANCELLED","Solicitação expirou sem confirmação antes do prazo.");}} catch(org.springframework.dao.DataAccessException unavailable){/* Next scheduled pass retries without logging session content. */}
    }

    private void suspendUpcoming(long mentor,long actor,String reason) {
        db.update("UPDATE career_mentor_profiles SET status='SUSPENDED',review_note=?,reviewed_by=?,reviewed_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP WHERE user_id=?",text(reason,500),actor>0?actor:null,mentor);
        db.update("UPDATE career_mentor_availability SET status='CLOSED' WHERE mentor_user_id=? AND status='OPEN'",mentor);
        var sessions=db.queryForList("UPDATE career_mentoring_sessions s SET status='CANCELLED',updated_at=CURRENT_TIMESTAMP FROM career_mentor_availability a WHERE a.id=s.availability_id AND s.mentor_user_id=? AND s.status IN ('REQUESTED','CONFIRMED') AND a.starts_at>CURRENT_TIMESTAMP RETURNING s.id,s.availability_id",mentor);
        for(var row:sessions){db.update("UPDATE career_mentor_availability SET status='CLOSED' WHERE id=?",row.get("availability_id"));event((UUID)row.get("id"),actor>0?actor:null,"CANCELLED","Sessão cancelada por medida de segurança e moderação.");}
    }

    private Map<String,Object> lockSession(long uid,UUID id) {
        var rows=db.queryForList("SELECT id,availability_id,mentor_user_id,mentee_user_id,status FROM career_mentoring_sessions WHERE id=? AND (mentor_user_id=? OR mentee_user_id=?) FOR UPDATE",id,uid,uid);if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Sessão não encontrada.");return rows.getFirst();
    }
    private void mentorApproved(long uid){if(!Boolean.TRUE.equals(db.queryForObject("SELECT EXISTS(SELECT 1 FROM career_mentor_profiles WHERE user_id=? AND status='APPROVED')",Boolean.class,uid)))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"O perfil de mentor precisa ser aprovado antes de oferecer horários.");}
    private List<String> specialties(long uid){return db.queryForList("SELECT specialty FROM career_mentor_specialties WHERE mentor_user_id=? ORDER BY specialty",String.class,uid);}
    private List<String> cleanTags(List<String> tags){if(tags==null||tags.isEmpty()||tags.size()>10)bad("Informe de uma a dez áreas de especialidade.");var cleaned=tags.stream().map(tag->text(tag,80)).filter(tag->!tag.isBlank()).map(tag->tag.replaceAll("\\s+"," ")).distinct().toList();if(cleaned.isEmpty()||cleaned.size()!=tags.size())bad("Remova especialidades vazias ou repetidas.");return cleaned;}
    private long adminId(String email){requireAdmin(email);return db.query("SELECT id FROM app_users WHERE LOWER(email)=LOWER(?)",(rs,n)->rs.getLong(1),email).stream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.FORBIDDEN,"A conta administradora precisa estar cadastrada em Oportunidades para registrar a auditoria."));}
    private void requireAdmin(String email){if(!admins.isSuperAdmin(email))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Apenas a administração da plataforma pode acessar esta função.");}
    private void event(UUID id,Long actor,String type,String details){db.update("INSERT INTO career_mentoring_events(session_id,actor_user_id,event_type,details) VALUES (?,?,?,?)",id,actor,type,text(details,500));}
    private Instant parse(String value){try{return Instant.parse(value);}catch(Exception invalid){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Informe data e hora com fuso no formato ISO-8601.");}}
    private boolean https(String value){try{URI uri=URI.create(value);return "https".equalsIgnoreCase(uri.getScheme())&&uri.getHost()!=null&&uri.getUserInfo()==null;}catch(Exception ignored){return false;}}
    private String text(String value,int max){if(value==null)return "";String safe=value.replaceAll("[\\p{Cntrl}&&[^\\n\\t]]","").trim();return safe.length()>max?safe.substring(0,max):safe;}
    private boolean containsContact(String value){if(value.contains("@")||value.matches("(?is).*\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b.*"))return true;String digits=value.replaceAll("\\D","");return digits.length()>=10&&digits.length()<=15;}
    private boolean containsPrivateLocation(String value){return value.matches("(?is).*\\b(rua|avenida|av\\.?|travessa|alameda|rodovia|estrada|quadra|lote|apartamento|apt\\.?|casa|nº|numero)\\s+[\\p{L}0-9 .,-]{1,80}\\b\\d{1,5}\\b.*");}
    private void bad(String message){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
}
