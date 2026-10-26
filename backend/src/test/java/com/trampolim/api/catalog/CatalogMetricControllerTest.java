package com.trampolim.api.catalog;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class CatalogMetricControllerTest {
    @Mock private CatalogMetricRepository metrics;
    @Mock private CatalogItemRepository items;
    @InjectMocks private CatalogMetricController controller;

    private org.springframework.mock.web.MockHttpServletRequest request() {
        var request = new org.springframework.mock.web.MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        return request;
    }

    @Test
    void storesAnonymousCatalogInteractionWithoutUserIdentifier() {
        when(metrics.save(any(CatalogMetric.class))).thenAnswer(invocation -> invocation.getArgument(0));
        controller.record("learner@example.com", new CatalogMetricController.MetricEvent("open", "course", "customer-service"), request());
        verify(metrics).save(any(CatalogMetric.class));
    }

    @Test
    void rejectsUnknownMetricEvent() {
        assertThrows(ResponseStatusException.class,
                () -> controller.record("learner@example.com", new CatalogMetricController.MetricEvent("login", "job", "1"), request()));
    }
}
