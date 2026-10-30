package com.trampolim.api.future;
import java.time.LocalDate;
import java.util.*;
import com.trampolim.api.radar.RadarModel;
import static com.trampolim.api.radar.RadarModel.*;
public final class FutureModel {
 private FutureModel(){}
 public record Demand(long id,String occupation,String description,String area,int estimatedHires,String educationDescription,String experienceDescription,String region,String modality,String hiringStart,String hiringEnd,String status,int revision,String company,Requirements requirements,String updatedAt){}
 public record Input(String occupation,String description,String area,int estimatedHires,String educationDescription,String experienceDescription,String region,String modality,String hiringStart,String hiringEnd,String status,int revision){}
 public record Interest(boolean consent,boolean notifyVacancies){}
 public record Plan(long courseId,long moduleId,String status){}
 public record SummaryRequest(long demandId,int requirementsRevision){}
 public record VacancyLink(long listingId){}
 public static Input validate(Input input){
  if(input==null||input.revision()<0||input.estimatedHires()<1||input.estimatedHires()>10000)bad("Informe uma estimativa entre 1 e 10.000 contratações.");
  if(text(input.occupation(),180).isEmpty()||text(input.description(),5000).isEmpty()||text(input.area(),120).isEmpty()||text(input.region(),180).isEmpty())bad("Informe cargo, descrição, área e região.");
  if(!Set.of("DRAFT","PUBLISHED","SUSPENDED","ARCHIVED").contains(Objects.toString(input.status(),""))||!Set.of("Presencial","Híbrido","Remoto","Online").contains(Objects.toString(input.modality(),"")))bad("Situação ou modalidade inválida.");
  try{LocalDate start=LocalDate.parse(input.hiringStart()),end=LocalDate.parse(input.hiringEnd());if(end.isBefore(start)||"PUBLISHED".equals(input.status())&&end.isBefore(LocalDate.now(java.time.ZoneId.of("America/Sao_Paulo"))))bad("Revise o período previsto; uma previsão vencida não pode ser publicada.");}catch(org.springframework.web.server.ResponseStatusException error){throw error;}catch(Exception error){bad("Informe datas válidas para a previsão.");}
  return new Input(text(input.occupation(),180),text(input.description(),5000),text(input.area(),120),input.estimatedHires(),text(input.educationDescription(),1000),text(input.experienceDescription(),1000),text(input.region(),180),input.modality(),input.hiringStart(),input.hiringEnd(),input.status(),input.revision());
 }
 public static Map<String,Object> band(long count){if(count<5)return Map.of("available",false,"label","Amostra insuficiente");long lower=count/5*5;return Map.of("available",true,"lower",lower,"upper",lower+4,"label",lower+"–"+(lower+4));}
}
