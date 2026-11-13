package com.trampolim.api.analytics;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProductAnalyticsServiceTest {
    @SuppressWarnings("unchecked")
    private Map<String,Object> firstSeries(Map<String,Object> metric) {
        return ((List<Map<String,Object>>)metric.get("series")).getFirst();
    }

    @Test void hidesCountsForGroupsSmallerThanFive() {
        var report=ProductMetricPrivacy.metric("applications","Candidaturas","count","count",
                List.of(Map.of("period","2026-11","value",4L,"contributors",4L)));
        var series=firstSeries(report);
        assertEquals(true,series.get("suppressed"));
        assertNull(series.get("value"));
        assertNull(series.get("contributors"));
    }

    @Test void publishesAggregatesAtTheMinimumCohortThreshold() {
        var report=ProductMetricPrivacy.metric("applications","Candidaturas","count","count",
                List.of(Map.of("period","2026-11","value",12L,"contributors",5L)));
        var series=firstSeries(report);
        assertEquals(false,series.get("suppressed"));
        assertEquals(12L,series.get("value"));
        assertEquals(5L,series.get("contributors"));
    }

    @Test void suppressesRatesWhenTheEligibleCohortIsTooSmall() {
        var report=ProductMetricPrivacy.metric("activation","Ativação","numerator / denominator","percentual",
                List.of(Map.of("period","2026-11","numerator",2L,"denominator",4L,"contributors",4L)));
        var series=firstSeries(report);
        assertEquals(true,series.get("suppressed"));
        assertNull(series.get("value"));
        assertNull(series.get("denominator"));
    }
}
