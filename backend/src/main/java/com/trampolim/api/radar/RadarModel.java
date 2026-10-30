package com.trampolim.api.radar;

import java.text.Normalizer;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Only explicit professional attributes enter the scoring model. */
public final class RadarModel {
    private RadarModel() {}
    public static String normalize(String value) {return Normalizer.normalize(Objects.toString(value,""),Normalizer.Form.NFD).replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT).trim().replaceAll("\\s+"," ");}
    public static String text(String value,int max){String v=Objects.toString(value,"").trim();if(v.length()>max)bad("Texto acima do limite.");return v;}
    public static void bad(String message){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
    public record Need(String skillCode,int level,boolean mandatory,boolean verified,boolean eliminatory) {}
    public record Requirements(int revision,List<Need> skills,Integer experienceMonths,Integer educationLevel,boolean experienceEliminatory,boolean educationEliminatory,String city,String modality,String area) {
        public static Requirements empty(){return new Requirements(0,List.of(),null,null,false,false,"","","");}
    }
    public record Level(int declared,int verified) {}
    public record Candidate(Map<String,Level> skills,Integer experienceMonths,Integer educationLevel,String city,String modality,String area) {}
    public record Policy(int revision,Map<String,Integer> weights) {}
    public record Job(String source,long id,String title,String company,String area,String location,String modality,String requirementsText,Requirements requirements,String updatedAt) {}
    public static final Set<String> FACTORS=Set.of("MANDATORY","DESIRABLE","EXPERIENCE","EDUCATION","LOCATION","MODALITY","PREFERENCES");
    public static Requirements validate(Requirements input){
        if(input==null||input.revision()<0||input.skills()==null||input.skills().size()>40)bad("Informe até 40 competências e a revisão atual.");
        Set<String> codes=new HashSet<>();
        for(var need:input.skills())if(need==null||need.skillCode()==null||!need.skillCode().matches("[a-z][a-z0-9-]{1,63}")||!codes.add(need.skillCode())||need.level()<1||need.level()>4||need.eliminatory()&&!need.mandatory())bad("Competência duplicada/inválida ou requisito eliminatório não obrigatório.");
        if(input.experienceMonths()!=null&&(input.experienceMonths()<0||input.experienceMonths()>600)||input.educationLevel()!=null&&(input.educationLevel()<1||input.educationLevel()>5))bad("Experiência ou escolaridade inválida.");
        if(input.experienceEliminatory()&&(input.experienceMonths()==null||input.experienceMonths()==0)||input.educationEliminatory()&&input.educationLevel()==null)bad("Informe o mínimo para um requisito eliminatório.");
        String modality=text(input.modality(),40);if(!modality.isEmpty()&&!Set.of("Presencial","Híbrido","Remoto","Online").contains(modality))bad("Modalidade inválida.");
        return new Requirements(input.revision(),List.copyOf(input.skills()),input.experienceMonths(),input.educationLevel(),input.experienceEliminatory(),input.educationEliminatory(),text(input.city(),180),modality,text(input.area(),120));
    }
    public static void validateWeights(Map<String,Integer> weights){if(weights==null||!weights.keySet().equals(FACTORS)||weights.values().stream().anyMatch(v->v==null||v<0||v>100)||weights.values().stream().mapToInt(Integer::intValue).sum()!=100||weights.get("MANDATORY")+weights.get("DESIRABLE")+weights.get("EXPERIENCE")+weights.get("EDUCATION")==0)bad("Use exatamente os sete fatores, soma 100 e peso profissional positivo.");}
}
