package com.trampolim.api.company;

import com.trampolim.api.radar.*;
import static com.trampolim.api.radar.RadarModel.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@ConditionalOnProperty(name="trampolim.datasource.empresas.enabled",havingValue="true")
public class CompanyRadarController {
 private final JdbcTemplate jdbc;private final RadarBridge bridge;
 public CompanyRadarController(@Qualifier("empresasJdbcTemplate")JdbcTemplate jdbc,RadarBridge bridge){this.jdbc=jdbc;this.bridge=bridge;}
 private void own(String email,long listing,boolean edit){
  Boolean allowed=jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM company_listings l JOIN company_members m ON m.company_id=l.company_id JOIN company_users u ON u.id=m.user_id WHERE l.id=? AND l.kind='job' AND LOWER(u.email)=LOWER(?) AND u.account_status='ACTIVE' AND u.email_verified=TRUE"+(edit?" AND m.role IN ('OWNER','RECRUITER')":"")+")",Boolean.class,listing,email);
  if(!Boolean.TRUE.equals(allowed))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Vaga indisponível para esta conta empresarial.");
 }
 @GetMapping("/api/company/listings/{id}/radar")public Requirements requirements(@AuthenticationPrincipal String email,@PathVariable long id){own(email,id,false);return stored(id);}
 @PutMapping("/api/company/listings/{id}/radar")@ResponseStatus(HttpStatus.NO_CONTENT)@Transactional("empresasTransactionManager")
 public void save(@AuthenticationPrincipal String email,@PathVariable long id,@RequestBody Requirements input){
  own(email,id,true);input=validate(input);bridge.validateSkills(input.skills().stream().map(Need::skillCode).toList());
  jdbc.queryForObject("SELECT id FROM company_listings WHERE id=? FOR UPDATE",Long.class,id);var previous=stored(id);
  if(previous.revision()!=input.revision())throw new ResponseStatusException(HttpStatus.CONFLICT,"Os critérios mudaram. Reabra a vaga antes de salvar.");
  var next=new Requirements(previous.revision()+1,input.skills(),input.experienceMonths(),input.educationLevel(),input.experienceEliminatory(),input.educationEliminatory(),input.city(),input.modality(),input.area());
  jdbc.update("INSERT INTO company_radar_requirements(listing_id,revision,definition) VALUES (?,?,?::jsonb) ON CONFLICT(listing_id) DO UPDATE SET revision=EXCLUDED.revision,definition=EXCLUDED.definition,updated_at=CURRENT_TIMESTAMP",id,next.revision(),bridge.encode(next));
  jdbc.update("INSERT INTO company_audit_events(company_id,actor_user_id,event_type) SELECT l.company_id,u.id,'RADAR_REQUIREMENTS_UPDATED' FROM company_listings l JOIN company_users u ON LOWER(u.email)=LOWER(?) WHERE l.id=?",email,id);
 }
 private Requirements stored(long id){var rows=jdbc.queryForList("SELECT definition FROM company_radar_requirements WHERE listing_id=?",id);return rows.isEmpty()?Requirements.empty():bridge.convert(bridge.read(rows.get(0).get("definition")),Requirements.class);}
 public static final String PUBLIC_JOB_CONDITION="c.verification_status='APPROVED' AND l.kind='job' AND l.publication_status='PUBLISHED' AND (l.expires_at IS NULL OR l.expires_at>=CURRENT_DATE) AND (l.application_deadline IS NULL OR l.application_deadline>=CURRENT_DATE) AND (l.is_internship=FALSE OR l.payment_status='PAID')";
 private String publicWhere(){return " FROM company_listings l JOIN companies c ON c.id=l.company_id LEFT JOIN company_radar_requirements r ON r.listing_id=l.id WHERE "+PUBLIC_JOB_CONDITION;}
 private List<Job> query(String suffix,Object... args){return jdbc.query("SELECT l.id,l.title,c.trade_name,l.area,l.location,l.modality,l.requirements,l.updated_at,r.definition"+publicWhere()+suffix,(rs,row)->new Job("company",rs.getLong(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6),rs.getString(7),rs.getObject(9)==null?Requirements.empty():bridge.convert(bridge.read(rs.getObject(9)),Requirements.class),rs.getTimestamp(8).toInstant().toString()),args);}
 @GetMapping("/api/public/companies/radar-jobs")public Map<String,Object> jobs(@RequestParam(defaultValue="9223372036854775807")long before,@RequestParam(defaultValue="20")int limit,@RequestParam(defaultValue="")String q){if(before<1||limit<1||limit>50||text(q,120).length()>120)bad("Consulta inválida.");var all=query(" AND l.id<? AND (?='' OR l.title ILIKE ? OR l.area ILIKE ?) ORDER BY l.id DESC LIMIT ?",before,q,"%"+q+"%","%"+q+"%",limit+1);var rows=all.stream().limit(limit).toList();var result=new LinkedHashMap<String,Object>();result.put("items",rows);result.put("nextCursor",all.size()>limit?rows.get(rows.size()-1).id():null);return result;}
 @GetMapping("/api/public/companies/radar-jobs/{id}")public Job job(@PathVariable long id){return query(" AND l.id=?",id).stream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"A vaga foi encerrada, expirou ou não está publicada."));}
}
