package com.trampolim.api.radar;

import java.util.*;
import static com.trampolim.api.radar.RadarModel.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.trampolim.api.security.PlatformAccess;

@Service
public class SkillCatalog {
    private final JdbcTemplate jdbc;private final PlatformAccess platform;
    public SkillCatalog(@Qualifier("jdbcTemplate")JdbcTemplate jdbc,PlatformAccess platform){this.jdbc=jdbc;this.platform=platform;}
    public record Input(String code,String name,String description,String category,String kind,boolean active,List<String> aliases){}
    public long user(String email){if(email==null)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);return jdbc.query("SELECT id FROM app_users WHERE LOWER(email)=LOWER(?) AND email_verified=TRUE AND account_type='PERSON'",(rs,row)->rs.getLong(1),email).stream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.FORBIDDEN,"Use uma conta verificada de Oportunidades."));}
    public void admin(String email){if(!platform.isSuperAdmin(email))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Somente a administração pode revisar o catálogo e a regra de pontuação.");}
    public List<Map<String,Object>> list(String q,String after,int limit){if(limit<1||limit>200||text(q,120).length()>120||text(after,64).length()>64)bad("Consulta inválida.");String key=normalize(q);var rows=jdbc.queryForList("SELECT s.*,(SELECT json_agg(t.display_name ORDER BY t.term) FROM skill_terms t WHERE t.skill_code=s.code) AS aliases FROM skill_concepts s WHERE s.active=TRUE AND s.code>? AND (?='' OR EXISTS(SELECT 1 FROM skill_terms t WHERE t.skill_code=s.code AND t.term LIKE ?)) ORDER BY s.code LIMIT ?",Objects.toString(after,""),key,"%"+key+"%",limit);for(var row:rows){Object aliases=row.get("aliases");try{row.put("aliases",aliases==null?List.of():new com.fasterxml.jackson.databind.ObjectMapper().readValue(aliases.toString(),List.class));}catch(Exception error){throw new IllegalStateException("Catálogo inválido.");}}return rows;}
    public List<Map<String,Object>> resolve(Collection<String> codes){if(codes==null||codes.size()>40||codes.stream().anyMatch(code->code==null||!code.matches("[a-z][a-z0-9-]{1,63}")))bad("Identificadores de competências inválidos.");if(codes.isEmpty())return List.of();String placeholders=String.join(",",Collections.nCopies(codes.size(),"?"));return jdbc.queryForList("SELECT code,name,description,category,kind,active,updated_at FROM skill_concepts WHERE code IN ("+placeholders+") ORDER BY code",codes.toArray());}
    public void validateCodes(Collection<String> codes){var rows=resolve(codes);if(rows.size()!=new HashSet<>(codes).size()||rows.stream().anyMatch(row->!Boolean.TRUE.equals(row.get("active"))))bad("Use competências ativas do catálogo.");}
    @Transactional("oportunidadesTransactionManager")public void save(String email,Input input){
        admin(email);if(input==null||input.code()==null||!input.code().matches("[a-z][a-z0-9-]{1,63}")||text(input.name(),180).isEmpty()||text(input.category(),80).isEmpty()||!Set.of("TECHNICAL","BEHAVIORAL").contains(Objects.toString(input.kind(),""))||input.aliases()==null||input.aliases().size()>30)bad("Revise a competência, a categoria e até 30 sinônimos.");
        var aliases=new LinkedHashMap<String,String>();for(String value:input.aliases()){String display=text(value,180);if(!display.isEmpty())aliases.put(normalize(display),display);}aliases.put(normalize(input.name()),input.name().trim());
        if(aliases.keySet().stream().anyMatch(key->key.matches("(?:idade(?: minima)?|sexo|genero|raca|etnia|religiao|estado civil|orientacao sexual|nacionalidade|deficiencia|gravidez)(?: .*)?")))bad("Características pessoais protegidas não são competências profissionais.");
        try {
            jdbc.update("INSERT INTO skill_concepts(code,name,description,category,kind,active) VALUES (?,?,?,?,?,?) ON CONFLICT(code) DO UPDATE SET name=EXCLUDED.name,description=EXCLUDED.description,category=EXCLUDED.category,kind=EXCLUDED.kind,active=EXCLUDED.active,updated_at=CURRENT_TIMESTAMP",input.code(),input.name().trim(),text(input.description(),2000),input.category().trim(),input.kind(),input.active());
            jdbc.update("DELETE FROM skill_terms WHERE skill_code=?",input.code());for(var alias:aliases.entrySet())jdbc.update("INSERT INTO skill_terms(term,skill_code,display_name) VALUES (?,?,?)",alias.getKey(),input.code(),alias.getValue());
        }catch(org.springframework.dao.DataIntegrityViolationException error){throw new ResponseStatusException(HttpStatus.CONFLICT,"Nome ou sinônimo já pertence a outra competência. Reutilize o identificador existente.");}
        audit(user(email),"CATALOG_UPDATED");
    }
    public void audit(long user,String event){jdbc.update("INSERT INTO radar_audit(actor_id,event) VALUES (?,?)",user,event);}
}
