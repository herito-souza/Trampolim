package com.trampolim.api.hub;
import java.util.*;import java.time.*;
import static com.trampolim.api.radar.RadarModel.*;
public final class HubRules {
 private HubRules(){}
 public record Profile(String legalName,String legalIdentifier,String contactEmail,String contactPhone,String responsibleName,String publicSlug,String headline,String about,String accentColor,boolean published,int revision,String institutionName){}
 public record Program(String title,String description,String startsOn,String endsOn,String status,List<Long> courseIds,int revision){}
 public record Campaign(String title,String body,String startsOn,String endsOn,String status,int revision){}
 public record Plan(String code,String name,String billingPeriod,int companyLimit,int programLimit,int memberLimit,int campaignLimit,String status){}
 public record Contract(long planId,String reference,String startsOn,String endsOn,String status,boolean confirmedAgreement,int revision){}
 public record DemandScope(long id,int revision){}
 public record CandidateQuery(long organizationId,List<DemandScope> demands,String from,String to){}
 public record ProgramScope(long id,List<Long> courseIds){}
 public record LearningQuery(long organizationId,List<ProgramScope> programs,String from,String to){}
 public record Consent(boolean consent){}
 public static Profile profile(Profile p){if(p==null||p.revision()<0)bad("Perfil inválido.");String email=text(p.contactEmail(),180);if(!email.isEmpty()&&!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))bad("Contato: informe e-mail válido.");String slug=text(p.publicSlug(),80);if(!slug.isEmpty()&&!slug.matches("[a-z0-9][a-z0-9-]{2,79}"))bad("Use um endereço público com letras minúsculas, números e hífen.");String color=text(p.accentColor(),7);if(!color.matches("#[a-fA-F0-9]{6}")||contrast(color,"#0b1421")<4.5)bad("A cor deve contrastar pelo menos 4,5:1 com o fundo do Trampolim.");if(p.published()&&(slug.isEmpty()||email.isEmpty()||text(p.headline(),180).isEmpty()))bad("Preencha endereço público, contato e apresentação antes de publicar.");return new Profile(text(p.legalName(),180),text(p.legalIdentifier(),80),email,text(p.contactPhone(),40),text(p.responsibleName(),120),slug,text(p.headline(),180),text(p.about(),5000),color.toLowerCase(Locale.ROOT),p.published(),p.revision(),text(p.institutionName(),180));}
 public static double contrast(String a,String b){double x=luminance(a),y=luminance(b);return (Math.max(x,y)+.05)/(Math.min(x,y)+.05);}
 private static double luminance(String hex){int n=Integer.parseInt(hex.substring(1),16);double[] c={(n>>16&255)/255d,(n>>8&255)/255d,(n&255)/255d};for(int i=0;i<3;i++)c[i]=c[i]<=.04045?c[i]/12.92:Math.pow((c[i]+.055)/1.055,2.4);return c[0]*.2126+c[1]*.7152+c[2]*.0722;}
 public static void dates(String from,String to){try{LocalDate a=LocalDate.parse(from),b=LocalDate.parse(to);if(b.isBefore(a)||b.isAfter(a.plusYears(2)))bad("Use um período válido de até dois anos.");}catch(org.springframework.web.server.ResponseStatusException e){throw e;}catch(Exception e){bad("Informe datas válidas.");}}
 public static void state(String state){if(!Set.of("DRAFT","PUBLISHED","ARCHIVED").contains(Objects.toString(state,"")))bad("Situação inválida.");}
 public static void courses(List<Long> ids){if(ids==null||ids.size()>20||ids.stream().anyMatch(id->id==null||id<1)||new HashSet<>(ids).size()!=ids.size())bad("Escolha até vinte cursos diferentes.");}
 public static void page(long before,int limit){if(before<1||limit<1||limit>50)bad("Use páginas de até cinquenta registros.");}
}
