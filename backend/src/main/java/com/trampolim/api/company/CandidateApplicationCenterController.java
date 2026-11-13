package com.trampolim.api.company;

import com.trampolim.api.radar.RadarBridge;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Bounded, minimal read contract for a verified Oportunidades account. Never returns recruiter notes or applicant contact data. */
@RestController
@RequestMapping("/api/integrations/application-center")
@ConditionalOnProperty(name="trampolim.datasource.empresas.enabled",havingValue="true")
public class CandidateApplicationCenterController {
    private static final Set<String> STAGES=Set.of("RECEIVED","IN_REVIEW","CONTACTED","INTERVIEW","ACCEPTED","DECLINED","CLOSED");
    private final JdbcTemplate db;
    private final RadarBridge bridge;
    public CandidateApplicationCenterController(@Qualifier("empresasJdbcTemplate") JdbcTemplate db,RadarBridge bridge){this.db=db;this.bridge=bridge;}

    public record Query(String verifiedEmail,String status,String company,String job,String from,String to,int page,int size){}
    public record Event(long id,String eventType,String previousStatus,String status,Instant createdAt){}
    public record Submission(long id,long listingId,String listingTitle,String companyName,String publicSlug,String location,
            String status,Instant createdAt,Instant statusUpdatedAt,List<Event> timeline){}
    public record Page(List<Submission> items,int page,int size,long totalElements,boolean hasNext){}
    private record Row(long id,long listingId,String title,String company,String slug,String location,String status,Instant createdAt){}

    @PostMapping("/query")
    public Page query(@RequestHeader(name="X-Trampolim-Bridge",required=false)String key,@RequestBody Query q){
        bridge.authorize(key);
        if(q==null||q.verifiedEmail()==null||q.verifiedEmail().length()>180||!q.verifiedEmail().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")||q.page()<0||q.page()>1000||q.size()<1||q.size()>50)bad();
        String status=q.status()==null||q.status().isBlank()?null:q.status().trim();
        if(status!=null&&!STAGES.contains(status))bad();
        String company=filter(q.company()),job=filter(q.job());
        LocalDate from=date(q.from()),to=date(q.to());if(from!=null&&to!=null&&from.isAfter(to))bad();
        var args=new ArrayList<Object>();StringBuilder where=new StringBuilder(" FROM company_applications a JOIN company_listings l ON l.id=a.listing_id JOIN companies c ON c.id=l.company_id WHERE LOWER(BTRIM(a.candidate_email))=LOWER(BTRIM(?)) AND a.consent_at IS NOT NULL");args.add(q.verifiedEmail().trim());
        if(status!=null){where.append(" AND a.status=?");args.add(status);}
        if(company!=null){where.append(" AND POSITION(LOWER(?) IN LOWER(c.trade_name))>0");args.add(company);}
        if(job!=null){where.append(" AND POSITION(LOWER(?) IN LOWER(l.title))>0");args.add(job);}
        var region=java.time.ZoneId.of("America/Sao_Paulo");
        if(from!=null){where.append(" AND a.created_at>=?");args.add(java.sql.Timestamp.from(from.atStartOfDay(region).toInstant()));}
        if(to!=null){where.append(" AND a.created_at<?");args.add(java.sql.Timestamp.from(to.plusDays(1).atStartOfDay(region).toInstant()));}
        long total=db.queryForObject("SELECT count(*)"+where,Long.class,args.toArray());
        var pageArgs=new ArrayList<>(args);pageArgs.add(q.size());pageArgs.add((long)q.page()*q.size());
        var rows=db.query("SELECT a.id,l.id,l.title,c.trade_name,c.public_slug,l.location,a.status,a.created_at"+where+" ORDER BY a.created_at DESC,a.id DESC LIMIT ? OFFSET ?",
                (rs,n)->new Row(rs.getLong(1),rs.getLong(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6),rs.getString(7),rs.getTimestamp(8).toInstant()),pageArgs.toArray());
        if(rows.isEmpty())return new Page(List.of(),q.page(),q.size(),total,(long)(q.page()+1)*q.size()<total);
        String marks=String.join(",",Collections.nCopies(rows.size(),"?"));var ids=rows.stream().map(Row::id).toList();
        String eventsSql="SELECT application_id,id,event_type,previous_status,status,created_at FROM (SELECT application_id,id,event_type,previous_status,status,created_at,ROW_NUMBER() OVER(PARTITION BY application_id ORDER BY id DESC) AS position FROM company_recruitment_events WHERE application_id IN ("+marks+") AND event_type IN ('RECEIVED','BASELINE','STAGE_CHANGED')) history WHERE position<=25 ORDER BY application_id,id";
        Map<Long,List<Event>> events=new HashMap<>();
        db.query(eventsSql,rs->{long id=rs.getLong(1);events.computeIfAbsent(id,ignored->new ArrayList<>()).add(new Event(rs.getLong(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getTimestamp(6).toInstant()));},ids.toArray());
        var items=rows.stream().map(r->{var timeline=List.copyOf(events.getOrDefault(r.id(),List.of()));Instant updated=timeline.isEmpty()?r.createdAt():timeline.getLast().createdAt();return new Submission(r.id(),r.listingId(),r.title(),r.company(),r.slug(),r.location(),r.status(),r.createdAt(),updated,timeline);}).toList();
        return new Page(items,q.page(),q.size(),total,(long)(q.page()+1)*q.size()<total);
    }
    private static String filter(String value){if(value==null||value.isBlank())return null;if(value.length()>120||value.indexOf('\0')>=0)bad();return value.trim();}
    private static LocalDate date(String value){if(value==null||value.isBlank())return null;try{var date=LocalDate.parse(value);if(date.getYear()<1900||date.getYear()>3000)bad();return date;}catch(Exception error){bad();return null;}}
    private static void bad(){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Filtros de candidaturas inválidos.");}
}
