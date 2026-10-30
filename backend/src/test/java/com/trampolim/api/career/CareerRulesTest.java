package com.trampolim.api.career;
import static org.junit.jupiter.api.Assertions.*;import org.junit.jupiter.api.Test;import org.springframework.web.server.ResponseStatusException;
class CareerRulesTest {
 @Test void suppressesSmallGroups(){assertEquals(false,CareerRules.band(9).get("available"));assertEquals("10–19",CareerRules.band(10).get("label"));assertFalse(CareerRules.band(13).containsKey("count"));}
 @Test void acceptsOnlySafeLinksWithoutServerFetch(){assertEquals("https://example.test/project",CareerRules.https("https://example.test/project"));for(String url:java.util.List.of("javascript:alert(1)","http://example.test","https://user:secret@example.test","file:///private"))assertThrows(ResponseStatusException.class,()->CareerRules.https(url));}
 @Test void removesContactAndSecretsBeforeAi(){String redacted=CareerRules.redact("Contato maria@example.test 11987654321 sk-syntheticSecret123456 AQ.syntheticSecret123456");assertFalse(redacted.contains("maria@"));assertFalse(redacted.contains("11987654321"));assertFalse(redacted.contains("syntheticSecret"));}
 @Test void validatesBoundedQueriesAndRegions(){assertEquals("SP",CareerRules.region("sp",false));assertThrows(ResponseStatusException.class,()->CareerRules.region("XX",false));assertThrows(ResponseStatusException.class,()->CareerRules.page(2,51));assertThrows(ResponseStatusException.class,()->CareerRules.dates("2026-01-01","2028-01-01"));}
}
