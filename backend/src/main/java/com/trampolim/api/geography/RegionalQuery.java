package com.trampolim.api.geography;
import java.util.*;import org.springframework.http.HttpStatus;import org.springframework.web.server.ResponseStatusException;
public final class RegionalQuery {
 private RegionalQuery(){}
 public record Filters(String region,String municipalityCode,boolean includeRemote,String q,String city,String area,String modality,long before,int limit){}
 public record Sql(String where,List<Object> args){}
 public static Sql filter(TerritorialCatalog geo,Filters f,String alias){return filter(geo,f,alias,false);}
 public static Sql filter(TerritorialCatalog geo,Filters f,String alias,boolean corporate){if(f==null||f.before()<1||f.limit()<1||f.limit()>50||f.q().length()>120||f.city().length()>120||f.area().length()>120||f.modality().length()>40)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Revise os filtros; use páginas de até cinquenta anúncios.");var args=new ArrayList<Object>();StringBuilder sql=new StringBuilder(" AND "+alias+".id<?");args.add(f.before());var codes=geo.selection(f.region(),f.municipalityCode());if(!codes.isEmpty()){var names=geo.legacyAliases(codes);sql.append(" AND (").append(alias).append(".municipality_code IN (").append(placeholders(codes.size())).append(") OR (").append(alias).append(".municipality_code IS NULL AND geo_normalize(").append(alias).append(".location) IN (").append(placeholders(names.size())).append("))");args.addAll(codes);args.addAll(names);if(f.includeRemote())sql.append(" OR geo_normalize(").append(alias).append(".modality) IN ('remoto','online')");sql.append(")");}
  if(!f.q().isBlank()){sql.append(" AND geo_normalize(concat_ws(' ',").append(alias).append(".title,").append(alias).append(".description,").append(alias).append(".location,").append(alias).append(".area,").append(corporate?"c.trade_name":alias+".provider").append(")) LIKE ?");args.add("%"+TerritorialCatalog.normalize(f.q())+"%");}
  if(!f.city().isBlank()){sql.append(" AND geo_normalize(").append(alias).append(".location) LIKE ?");args.add("%"+TerritorialCatalog.normalize(f.city())+"%");}
  if(!f.area().isBlank()){var names=areaNames(f.area());sql.append(" AND geo_normalize(").append(alias).append(".area) IN (").append(placeholders(names.size())).append(")");args.addAll(names);}
  if(!f.modality().isBlank()){sql.append(" AND geo_normalize(").append(alias).append(".modality)=?");args.add(TerritorialCatalog.normalize(f.modality()));}
  return new Sql(sql.toString(),args);
 }
 public static List<String> areaNames(String area){String key=TerritorialCatalog.normalize(area);return switch(key){
 case "commerce", "comercio", "sales" -> List.of("commerce","comercio","sales","vendas");
 case "services", "servicos", "service" -> List.of("services","servicos","service","atendimento");
 case "agribusiness", "agronegocio" -> List.of("agribusiness","agronegocio");
 case "health", "saude" -> List.of("health","saude","saude e bem estar");
 case "education", "educacao" -> List.of("education","educacao");
 case "admin", "administracao" -> List.of("admin","administracao");
 case "technology", "tecnologia" -> List.of("technology","tecnologia","tecnologia e informatica");
 case "logistics", "logistica" -> List.of("logistics","logistica");
 case "industry", "industria", "production" -> List.of("industry","industria","production","producao");
 default -> List.of(key);};}
 public static String placeholders(int count){return String.join(",",Collections.nCopies(count,"?"));}
 public static Map<String,Object> page(List<Map<String,Object>> rows,int limit){var page=new LinkedHashMap<String,Object>();page.put("items",rows.stream().limit(limit).toList());page.put("nextCursor",rows.size()>limit?rows.get(limit-1).get("id"):null);page.put("notice","Local de trabalho informado no anúncio. Registros antigos sem município padronizado entram somente por correspondência exata não ambígua; localidades desconhecidas não são atribuídas à sede da empresa.");return page;}
}
