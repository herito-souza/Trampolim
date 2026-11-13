package com.trampolim.api.career;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class CareerEligibilityControllerTest {
    @Test void legalGuidanceKeepsSeparateRegimesAndOfficialSources() {
        var guidance = new CareerEligibilityController(null).guidance();
        assertEquals("2026-11-13", guidance.get("reviewedAt"));
        assertTrue(guidance.get("notice").toString().contains("não aprova candidatura"));
        var regimes = (java.util.List<java.util.Map<String, Object>>) guidance.get("regimes");
        assertEquals(3, regimes.size());
        assertTrue(regimes.stream().anyMatch(item -> item.get("regime").equals("INTERNSHIP")
                && item.get("source").toString().contains("planalto.gov.br")));
        assertTrue(regimes.stream().anyMatch(item -> item.get("regime").equals("APPRENTICESHIP")
                && item.get("source").toString().contains("gov.br/trabalho-e-emprego")));
    }
}
