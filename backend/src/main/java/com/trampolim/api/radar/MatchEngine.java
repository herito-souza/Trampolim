package com.trampolim.api.radar;

import java.util.*;
import static com.trampolim.api.radar.RadarModel.*;

/** Deterministic, bounded, no personal identities, sensitive attributes or free-text inference. */
public final class MatchEngine {
    public Map<String,Object> evaluate(Candidate candidate,Requirements requirements,Policy policy){
        validate(requirements);validateWeights(policy.weights());
        List<Map<String,Object>> rows=new ArrayList<>(),factors=new ArrayList<>();List<String> blockers=new ArrayList<>(),pending=new ArrayList<>();
        for(var need:requirements.skills()){
            Level level=candidate.skills().getOrDefault(need.skillCode(),new Level(0,0));int used=need.verified()?level.verified():Math.max(level.declared(),level.verified());
            String state=used>=need.level()?"MET":used>0?"PARTIAL":"MISSING";
            var row=new LinkedHashMap<String,Object>();row.put("skillCode",need.skillCode());row.put("requiredLevel",need.level());row.put("declaredLevel",level.declared());row.put("verifiedLevel",level.verified());row.put("usedLevel",used);row.put("requiresEvidence",need.verified());row.put("mandatory",need.mandatory());row.put("eliminatory",need.eliminatory());row.put("status",state);row.put("credit",Math.min(1d,(double)used/need.level()));rows.add(row);
            if(need.eliminatory()&&!state.equals("MET"))blockers.add("Competência eliminatória não demonstrada: "+need.skillCode()+", nível "+need.level()+(need.verified()?" comprovado por avaliação.":" declarado ou comprovado."));
        }
        double earned=0,available=0;int known=0,configured=0;
        for(String key:List.of("MANDATORY","DESIRABLE","EXPERIENCE","EDUCATION","LOCATION","MODALITY","PREFERENCES")){
            Double credit=null;boolean applicable=false,unknown=false;String explanation="Não exigido nesta vaga.";
            switch(key){
                case "MANDATORY","DESIRABLE" -> {boolean mandatory=key.equals("MANDATORY");var selected=rows.stream().filter(row->row.get("mandatory").equals(mandatory)).toList();applicable=!selected.isEmpty();if(applicable){credit=selected.stream().mapToDouble(row->((Number)row.get("credit")).doubleValue()).average().orElse(0);explanation="Média de min(nível demonstrado / nível solicitado, 1). Declarações não atendem exigências de comprovação.";}}
                case "EXPERIENCE" -> {applicable=requirements.experienceMonths()!=null&&requirements.experienceMonths()>0;if(applicable){unknown=candidate.experienceMonths()==null;credit=unknown?0:Math.min(1d,(double)candidate.experienceMonths()/requirements.experienceMonths());explanation="Experiência geral declarada: "+Objects.toString(candidate.experienceMonths(),"não informada")+" meses; mínimo "+requirements.experienceMonths()+". Não comprova atuação em área específica.";if(requirements.experienceEliminatory()&&(unknown||credit<1)) (unknown?pending:blockers).add("Experiência eliminatória: "+explanation);}}
                case "EDUCATION" -> {applicable=requirements.educationLevel()!=null;if(applicable){unknown=candidate.educationLevel()==null;credit=!unknown&&candidate.educationLevel()>=requirements.educationLevel()?1d:0d;explanation="Escolaridade concluída reconhecida no perfil: "+Objects.toString(candidate.educationLevel(),"não informada")+"; mínimo "+requirements.educationLevel()+". Informação declarada, sujeita à revisão humana.";if(requirements.educationEliminatory()&&(unknown||credit<1))(unknown?pending:blockers).add("Escolaridade eliminatória: "+explanation);}}
                case "LOCATION" -> {applicable=!normalize(requirements.city()).isEmpty()&&!Set.of("remoto","online").contains(normalize(requirements.modality()));if(applicable){unknown=normalize(candidate.city()).isEmpty();credit=normalize(candidate.city()).equals(normalize(requirements.city()))?1d:0d;explanation="Preferência de cidade comparada literalmente; não usa origem, endereço residencial ou nacionalidade. Trabalho remoto não recebe penalidade de cidade.";}}
                case "MODALITY" -> {applicable=!normalize(requirements.modality()).isEmpty()&&!normalize(candidate.modality()).isEmpty();if(applicable){credit=normalize(requirements.modality()).equals(normalize(candidate.modality()))?1d:0d;explanation="Modalidade da vaga comparada à preferência voluntária de trabalho.";}}
                case "PREFERENCES" -> {applicable=!normalize(requirements.area()).isEmpty()&&!normalize(candidate.area()).isEmpty();if(applicable){credit=normalize(requirements.area()).equals(normalize(candidate.area()))?1d:0d;explanation="Área da vaga comparada à preferência profissional voluntária.";}}
            }
            int weight=policy.weights().get(key);if(applicable&&weight>0){configured++;if(!unknown)known++;available+=weight;earned+=weight*credit;if(unknown)pending.add("Informação pendente para o fator "+key+"; recebe zero enquanto não informada.");}
            var factor=new LinkedHashMap<String,Object>();factor.put("key",key);factor.put("weight",weight);factor.put("applicable",applicable);factor.put("credit",credit==null?0:credit);factor.put("status",!applicable?"NOT_APPLICABLE":unknown?"UNKNOWN":credit>=1?"MET":credit>0?"PARTIAL":"NOT_MET");factor.put("explanation",explanation);factors.add(factor);
        }
        boolean professional=rows.size()>0||requirements.experienceMonths()!=null&&requirements.experienceMonths()>0||requirements.educationLevel()!=null;
        var result=new LinkedHashMap<String,Object>();result.put("score",professional&&available>0?Math.round(earned/available*10000d)/100d:null);result.put("status",professional&&available>0?"ANALYZED":"INSUFFICIENT_STRUCTURED_DATA");result.put("policyRevision",policy.revision());result.put("requirementsRevision",requirements.revision());result.put("factors",factors);result.put("skills",rows);result.put("eliminatoryNotDemonstrated",blockers);result.put("pendingInformation",pending);result.put("knownFactors",known);result.put("configuredFactors",configured);result.put("formula","100 × soma(peso × crédito) / soma dos pesos aplicáveis; crédito ausente = 0. Não há rejeição automática.");result.put("notice","Análise orientativa, sem garantia de contratação. Informações declaradas e critérios devem ser revisados por pessoas.");return result;
    }
}
