package com.trampolim.api.geography;
import static org.junit.jupiter.api.Assertions.*;import org.junit.jupiter.api.Test;import org.springframework.core.io.DefaultResourceLoader;import org.springframework.web.server.ResponseStatusException;import java.util.*;
class TerritorialCatalogTest {
 private TerritorialCatalog catalog()throws Exception{return new TerritorialCatalog(new DefaultResourceLoader(),"classpath:geography/regions.json");}
 @Test void officialReferenceIsLocalAndAccentInsensitive()throws Exception{var geo=catalog();assertEquals("Irecê",geo.municipality("2914604").name());assertEquals("2914604",geo.search("irece","BA",20).get(0).code());assertEquals("lapao",TerritorialCatalog.normalize("Lapão"));assertEquals(20,geo.region("irece-ba").municipalityCodes().size());assertEquals("SP",geo.municipality("3550308").state());assertThrows(ResponseStatusException.class,()->geo.municipality("9999999"));}
 @Test void ambiguityAndInvalidScopesDoNotBecomeAutomaticAssignments()throws Exception{var geo=catalog();var names=geo.search("São Domingos","",50);var code=names.stream().filter(c->c.name().equals("São Domingos")).findFirst().orElseThrow().code();assertFalse(geo.legacyAliases(List.of(code)).contains("sao domingos"));assertTrue(geo.legacyAliases(List.of("2914604")).contains("irece"));assertThrows(ResponseStatusException.class,()->geo.selection("irece-ba","2914604"));assertThrows(ResponseStatusException.class,()->geo.selection("unknown",""));assertThrows(ResponseStatusException.class,()->RegionalQuery.filter(geo,new RegionalQuery.Filters("","",true,"","","","",100,51),"l"));}
 @Test void referenceAndRegionsCanExpandThroughConfiguration(@org.junit.jupiter.api.io.TempDir java.nio.file.Path directory)throws Exception{
  var config=directory.resolve("regions-qa.json");
  java.nio.file.Files.writeString(config,"{\"defaultRegion\":\"synthetic-sp-qa\",\"referenceMunicipality\":\"3550308\",\"source\":\"Synthetic QA configuration using official municipality code\",\"regions\":[{\"id\":\"synthetic-sp-qa\",\"name\":\"Região sintética QA\",\"description\":\"Configuration test only\",\"municipalityCodes\":[\"3550308\"]}]}");
  var geo=new TerritorialCatalog(new DefaultResourceLoader(),config.toUri().toString());
  assertEquals("synthetic-sp-qa",geo.settings().get("defaultRegion"));
  assertEquals("São Paulo",((TerritorialCatalog.Municipality)geo.settings().get("referenceMunicipality")).name());
  assertEquals(List.of("3550308"),geo.selection("synthetic-sp-qa",""));
  assertEquals("Irecê",geo.municipality("2914604").name());
 }
}
