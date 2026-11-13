package com.trampolim.api.career;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class CareerInterviewQuestionsTest {
    @Test void createsDifferentQuestionsForInterviewTypes() {
        var firstJob = CareerInterviewQuestions.create("Atendente", "ENTRY", "FIRST_JOB");
        var technical = CareerInterviewQuestions.create("Atendente", "ENTRY", "TECHNICAL");
        assertEquals(3, firstJob.size());
        assertTrue(firstJob.getFirst().contains("Atendente"));
        assertNotEquals(firstJob, technical);
    }

    @Test void appliesExperienceLevelWithoutRankingTheCandidate() {
        var entry = CareerInterviewQuestions.create("Analista", "ENTRY", "BEHAVIORAL");
        var senior = CareerInterviewQuestions.create("Analista", "SENIOR", "BEHAVIORAL");
        assertTrue(entry.getFirst().contains("início de carreira"));
        assertTrue(senior.getFirst().contains("maior autonomia"));
    }

    @Test void rejectsUnsupportedContext() {
        assertThrows(IllegalArgumentException.class, () -> CareerInterviewQuestions.create("Analista", "UNKNOWN", "GENERAL"));
        assertThrows(IllegalArgumentException.class, () -> CareerInterviewQuestions.create("Analista", "ENTRY", "UNKNOWN"));
    }
}
