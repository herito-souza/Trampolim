package com.trampolim.api.fairs;
import java.time.*;import java.util.*;
import org.springframework.http.HttpStatus;import org.springframework.web.server.ResponseStatusException;
public final class FairRules {
 private FairRules(){}
 public record Definition(String title,String description,String startsAt,String endsAt,String modality,String municipalityCode,String venue,boolean registrationEnabled,Long campaignId,long expectedRevision,UUID requestKey){}
 public record Version(long expectedRevision){}
 public record Publication(String status,long expectedRevision){}
 public record Reference(long id,long expectedRevision){}
 public record CompanyDecision(boolean accepted,long expectedRevision){}
 public record Session(String kind,String title,String description,String speakerName,boolean speakerConsent,String startsAt,String endsAt,long expectedRevision){}
 public record Registration(boolean consent,long expectedRevision,UUID requestKey){}
 public record CountQuery(long organizationId,List<Long> eventIds){}
 public static String text(String value,int max,boolean required){String result=value==null?"":value.trim();if(result.length()>max||result.indexOf(0)>=0||required&&result.isBlank())bad("Revise os campos obrigatórios e seus limites.");return result;}
 public static Instant instant(String value){try{return Instant.parse(value);}catch(Exception e){bad("Informe data e horário com fuso no formato ISO-8601.");return null;}}
 public static void period(Instant from,Instant to){if(!to.isAfter(from))bad("O encerramento deve ocorrer após o início.");}
 public static void page(long before,int limit){if(before<1||limit<1||limit>50)bad("Paginação inválida. Use de 1 a 50 registros.");}
 public static long number(Map<String,Object> row,String key){return ((Number)row.get(key)).longValue();}
 public static Instant time(Object value){return value instanceof java.sql.Timestamp t?t.toInstant():Instant.parse(value.toString());}
 public static void bad(String message){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
 public static void conflict(){throw new ResponseStatusException(HttpStatus.CONFLICT,"O registro mudou ou não permite esta operação. Consulte a versão atual.");}
 public static void missing(){throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Evento ou recurso não encontrado neste contexto.");}
}
