package com.trampolim.api.fairs;
import org.junit.jupiter.api.Test;import static org.junit.jupiter.api.Assertions.*;import java.time.Instant;
class FairRulesTest {
 @Test void datesRequireAnOffsetAndValidOrder(){assertEquals(Instant.parse("2026-11-10T12:00:00Z"),FairRules.instant("2026-11-10T09:00:00-03:00"));assertThrows(org.springframework.web.server.ResponseStatusException.class,()->FairRules.instant("2026-11-10T09:00"));assertThrows(org.springframework.web.server.ResponseStatusException.class,()->FairRules.period(Instant.EPOCH,Instant.EPOCH));}
 @Test void textAndPaginationAreBounded(){assertEquals("Irecê",FairRules.text(" Irecê ",180,true));assertThrows(org.springframework.web.server.ResponseStatusException.class,()->FairRules.text("x".repeat(181),180,true));assertThrows(org.springframework.web.server.ResponseStatusException.class,()->FairRules.page(1,51));assertThrows(org.springframework.web.server.ResponseStatusException.class,()->FairRules.page(0,20));}
}
