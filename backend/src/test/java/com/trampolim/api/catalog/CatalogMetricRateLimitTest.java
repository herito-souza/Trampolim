package com.trampolim.api.catalog;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;
import com.trampolim.api.security.PlatformAccess;
class CatalogMetricRateLimitTest {
 @Test void existingPerAddressBudgetIsPreserved(){var metrics=mock(CatalogMetricRepository.class);var controller=new CatalogMetricController(metrics,mock(CatalogItemRepository.class),mock(PlatformAccess.class));var request=new MockHttpServletRequest();request.setRemoteAddr("synthetic-address");var event=new CatalogMetricController.MetricEvent("open","course","synthetic-course");for(int i=0;i<240;i++)controller.record("fixture@example.test",event,request);assertEquals(429,assertThrows(ResponseStatusException.class,()->controller.record("fixture@example.test",event,request)).getStatusCode().value());verify(metrics,times(240)).save(any());}
}
