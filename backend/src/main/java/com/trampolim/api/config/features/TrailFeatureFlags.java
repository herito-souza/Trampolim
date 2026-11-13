package com.trampolim.api.config.features;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

/** One instance per trail, bound to only that trail's database and transaction manager. */
public final class TrailFeatureFlags {
    public record Change(boolean enabled,int expectedRevision,List<String> environments,List<String> profiles,
            List<Long> companyIds,List<String> municipalityCodes,String reason){}
    private final String trail,environment;private final JdbcTemplate db;private final TransactionTemplate tx;private final ObjectMapper json;
    public TrailFeatureFlags(String trail,String environment,JdbcTemplate db,TransactionTemplate tx,ObjectMapper json){this.trail=trail;this.environment=environment;this.db=db;this.tx=tx;this.json=json;}
    public String trail(){return trail;}
    public List<FeatureFlag> list(){return db.query("SELECT flag_key,enabled,revision,updated_at,updated_by,targeting FROM feature_flags ORDER BY flag_key",(r,n)->read(r.getString(1),r.getBoolean(2),r.getInt(3),r.getTimestamp(4).toInstant().toString(),r.getString(5),r.getString(6)));}
    public List<Map<String,Object>> audit(String key){return db.queryForList("SELECT action,reason,actor_email,previous_state,current_state,created_at FROM feature_flag_audit WHERE flag_key=? ORDER BY id DESC LIMIT 100",key);}
    public boolean enabled(String key,String profile,Long companyId,String municipality){return FeatureFlagTargeting.matches(find(key),environment,profile,companyId,municipality);}
    public List<Map<String,Object>> availableFor(String email,long userId){
        List<String> profiles=new ArrayList<>();List<Long> companies=List.of();
        if(trail.equals("OPORTUNIDADES")){
            if(db.queryForObject("SELECT count(*) FROM app_users WHERE id=? AND LOWER(email)=LOWER(?)",Integer.class,userId,email)==0)return List.of();
            profiles.add("CANDIDATE");if(db.queryForObject("SELECT count(*) FROM platform_roles WHERE user_id=? AND role='SUPER_ADMIN'",Integer.class,userId)>0)profiles.add("ADMIN");
        }else if(trail.equals("EMPRESAS")){
            if(db.queryForObject("SELECT count(*) FROM company_users WHERE id=? AND LOWER(email)=LOWER(?) AND account_status='ACTIVE' AND email_verified=TRUE",Integer.class,userId,email)==0)return List.of();
            var rows=db.queryForList("SELECT c.id,CASE WHEN c.owner_user_id=? THEN 'COMPANY_OWNER' ELSE m.role END AS role FROM companies c LEFT JOIN company_members m ON m.company_id=c.id AND m.user_id=? WHERE c.owner_user_id=? OR m.user_id=?",userId,userId,userId,userId);
            companies=rows.stream().map(r->((Number)r.get("id")).longValue()).distinct().toList();
            for(var row:rows){String role=Objects.toString(row.get("role"),"").toUpperCase(Locale.ROOT);if(role.equals("OWNER")||role.equals("COMPANY_OWNER"))profiles.add("COMPANY_OWNER");if(role.equals("RECRUITER")||role.equals("ADMIN"))profiles.add("RECRUITER");}
        }else{
            if(db.queryForObject("SELECT count(*) FROM student_users WHERE id=? AND LOWER(email)=LOWER(?) AND account_status='ACTIVE' AND email_verified=TRUE",Integer.class,userId,email)==0)return List.of();
            profiles.add("STUDENT");for(String role:db.queryForList("SELECT role FROM learning_account_roles WHERE user_id=?",String.class,userId)){if(role.equals("TEACHER"))profiles.add("TEACHER");if(role.equals("ADMIN"))profiles.add("ADMIN");}
        }
        List<String> resolvedProfiles=List.copyOf(profiles);List<Long> resolvedCompanies=companies;
        return list().stream().filter(flag->resolvedProfiles.stream().anyMatch(profile->FeatureFlagTargeting.matches(flag,environment,profile,null,null))||resolvedCompanies.stream().anyMatch(company->resolvedProfiles.stream().anyMatch(profile->FeatureFlagTargeting.matches(flag,environment,profile,company,null))))
                .map(flag->Map.<String,Object>of("key",flag.key(),"enabled",true,"revision",flag.revision())).toList();
    }
    public FeatureFlag find(String key){return db.query("SELECT flag_key,enabled,revision,updated_at,updated_by,targeting FROM feature_flags WHERE flag_key=?",(r,n)->read(r.getString(1),r.getBoolean(2),r.getInt(3),r.getTimestamp(4).toInstant().toString(),r.getString(5),r.getString(6)),key).stream().findFirst().orElse(null);}
    public FeatureFlag update(String key,String actor,Change c){validate(key,actor,c);return tx.execute(status->{
        FeatureFlag before=db.query("SELECT flag_key,enabled,revision,updated_at,updated_by,targeting FROM feature_flags WHERE flag_key=? FOR UPDATE",(r,n)->read(r.getString(1),r.getBoolean(2),r.getInt(3),r.getTimestamp(4).toInstant().toString(),r.getString(5),r.getString(6)),key).stream().findFirst().orElse(null);
        int revision=before==null?0:before.revision();if(c.expectedRevision()!=revision)throw new ResponseStatusException(HttpStatus.CONFLICT,"A flag mudou; recarregue antes de editar.");
        Map<String,Object> target=Map.of("environments",c.environments().stream().map(v->v.toLowerCase(Locale.ROOT)).toList(),"profiles",c.profiles().stream().map(v->v.toUpperCase(Locale.ROOT)).toList(),"companyIds",c.companyIds(),"municipalityCodes",c.municipalityCodes());
        try{String targeting=json.writeValueAsString(target);if(before==null)db.update("INSERT INTO feature_flags(flag_key,enabled,revision,updated_by,targeting) VALUES (?,?,1,?,?::jsonb)",key,c.enabled(),actor,targeting);else db.update("UPDATE feature_flags SET enabled=?,revision=revision+1,updated_by=?,targeting=?::jsonb,updated_at=CURRENT_TIMESTAMP WHERE flag_key=? AND revision=?",c.enabled(),actor,targeting,key,revision);
            Map<String,Object> current=Map.of("key",key,"enabled",c.enabled(),"revision",revision+1,"updatedBy",actor,"targeting",target);
            db.update("INSERT INTO feature_flag_audit(flag_key,action,reason,actor_email,previous_state,current_state) VALUES (?,?,?,?,?::jsonb,?::jsonb)",key,before==null?"CREATED":"UPDATED",c.reason().trim(),actor,before==null?"{}":json.writeValueAsString(before),json.writeValueAsString(current));return find(key);
        }catch(Exception e){throw new IllegalStateException("Could not persist feature flag change",e);}
    });}
    private FeatureFlag read(String key,boolean enabled,int revision,String at,String by,String raw){try{JsonNode n=json.readTree(raw);return new FeatureFlag(key,enabled,revision,at,by,strings(n,"environments",true),strings(n,"profiles",false),longs(n,"companyIds"),strings(n,"municipalityCodes",false));}catch(Exception e){throw new IllegalStateException("Invalid feature flag targeting",e);}}
    private List<String> strings(JsonNode n,String field,boolean lower){List<String> out=new ArrayList<>();n.path(field).forEach(v->out.add(lower?v.asText().toLowerCase(Locale.ROOT):v.asText().toUpperCase(Locale.ROOT)));return out;}
    private List<Long> longs(JsonNode n,String field){List<Long> out=new ArrayList<>();n.path(field).forEach(v->out.add(v.asLong()));return out;}
    private static void validate(String key,String actor,Change c){if(key==null||!key.matches("[a-z][a-z0-9.-]{1,79}")||actor==null||actor.isBlank()||c==null||c.expectedRevision()<0||c.environments()==null||c.profiles()==null||c.companyIds()==null||c.municipalityCodes()==null||c.reason()==null||c.reason().trim().length()<10||c.reason().length()>500)bad();
        if(c.environments().isEmpty()||c.environments().size()>5||c.environments().stream().anyMatch(v->!Set.of("local","dev","test","staging","prod").contains(v.toLowerCase(Locale.ROOT))))bad();
        if(c.profiles().isEmpty()||c.profiles().size()>7||c.profiles().stream().anyMatch(v->!FeatureFlagTargeting.PROFILES.contains(v.toUpperCase(Locale.ROOT))))bad();
        if(c.companyIds().size()>100||c.companyIds().stream().anyMatch(v->v==null||v<1)||new HashSet<>(c.companyIds()).size()!=c.companyIds().size())bad();
        if(c.municipalityCodes().size()>100||c.municipalityCodes().stream().anyMatch(v->v==null||!v.matches("[0-9]{7}"))||new HashSet<>(c.municipalityCodes()).size()!=c.municipalityCodes().size())bad();}
    private static void bad(){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Revise ambientes, perfis e alvos. Informe justificativa de 10 a 500 caracteres.");}
}
