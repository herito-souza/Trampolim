package com.trampolim.api.geography;

import java.util.*;
import java.text.Normalizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Public geographical reference, never a source of account or tenant permissions. */
@Component
public class TerritorialCatalog {
 public record Municipality(String code,String name,String state) {}
 public record Region(String id,String name,String description,List<String> municipalityCodes) {}
 private record Municipalities(String source,String retrievedAt,List<Municipality> municipalities) {}
 private record Regions(String defaultRegion,String referenceMunicipality,String source,List<Region> regions) {}
 private final List<Municipality> cities;private final Map<String,Municipality> byCode;private final Regions settings;private final Set<String> uniqueNames;
 public TerritorialCatalog(ResourceLoader loader,@Value("${geography.regions:classpath:geography/regions.json}")String config) throws java.io.IOException {
  var json=new ObjectMapper();try(var stream=loader.getResource("classpath:geography/municipalities.json").getInputStream()){cities=List.copyOf(json.readValue(stream,Municipalities.class).municipalities());}
  var codes=new LinkedHashMap<String,Municipality>();var counts=new HashMap<String,Integer>();for(var city:cities){if(!city.code().matches("[0-9]{7}")||codes.put(city.code(),city)!=null)throw new IllegalStateException("Duplicate municipality reference");counts.merge(normalize(city.name()),1,Integer::sum);}byCode=Map.copyOf(codes);uniqueNames=new HashSet<>();counts.forEach((name,count)->{if(count==1)uniqueNames.add(name);});
  try(var stream=loader.getResource(config).getInputStream()){settings=json.readValue(stream,Regions.class);}
  var ids=new HashSet<String>();for(var region:settings.regions())if(region.id()==null||!region.id().matches("[a-z0-9-]{1,60}")||!ids.add(region.id())||region.municipalityCodes()==null||region.municipalityCodes().isEmpty()||region.municipalityCodes().size()>600||!byCode.keySet().containsAll(region.municipalityCodes())||new HashSet<>(region.municipalityCodes()).size()!=region.municipalityCodes().size())throw new IllegalStateException("Invalid territorial configuration");
  if(!ids.contains(settings.defaultRegion())||!byCode.containsKey(settings.referenceMunicipality()))throw new IllegalStateException("Invalid default territory");
 }
 public static String normalize(String value){return Normalizer.normalize(Objects.toString(value,""),Normalizer.Form.NFD).replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+"," ").trim();}
 public Municipality municipality(String code){var city=byCode.get(code);if(city==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Selecione um município válido.");return city;}
 public String label(Object code){if(code==null)return null;var city=byCode.get(code.toString());return city==null?null:city.name()+" — "+city.state();}
 public Region region(String id){return settings.regions().stream().filter(r->r.id().equals(id)).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Região não configurada."));}
 public Map<String,Object> settings(){return Map.of("defaultRegion",settings.defaultRegion(),"referenceMunicipality",municipality(settings.referenceMunicipality()),"regions",settings.regions(),"source",settings.source(),"states",states(),"municipalitySource","https://servicodados.ibge.gov.br/api/v1/localidades/municipios");}
 public List<Municipality> search(String query,String state,int limit){if(query==null||query.length()>120||state==null||!state.matches("[A-Z]{2}|" )||limit<1||limit>50)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Consulta de municípios inválida.");String q=normalize(query);return cities.stream().filter(c->(state.isEmpty()||state.equals(c.state()))&&(q.isEmpty()||normalize(c.name()).contains(q)||c.code().equals(q))).limit(limit).toList();}
 public List<String> selection(String region,String code){if(!region.isEmpty()&&!code.isEmpty())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Escolha região ou município.");if(!code.isEmpty())return List.of(municipality(code).code());return region.isEmpty()?List.of():region(region).municipalityCodes();}
 public List<String> states(){return cities.stream().map(Municipality::state).distinct().sorted().toList();}
 public List<String> stateCodes(String state){if(!states().contains(state))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Estado inválido.");return cities.stream().filter(c->c.state().equals(state)).map(Municipality::code).toList();}
 public List<String> legacyAliases(List<String> codes){var names=new LinkedHashSet<String>();for(String code:codes){var city=municipality(code);String name=normalize(city.name());names.add(name+" "+city.state().toLowerCase(Locale.ROOT));if(uniqueNames.contains(name))names.add(name);}return List.copyOf(names);}
}
