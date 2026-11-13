package com.trampolim.api.config;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
class TrailReadinessTest {
 @Test void secondaryFailureFailsReadiness(){assertStatus("DOWN","UP","DOWN","UP");}
 @Test void disabledTrailDoesNotFailReadiness(){assertStatus("UP","UP","DISABLED","UP");}
 @Test void primaryFailureFailsReadiness(){assertStatus("DOWN","DOWN","UP","UP");}
 @Test void healthyTrailsAreReady(){assertStatus("UP","UP","UP","UP");}
 private void assertStatus(String expected,String a,String b,String c){var controller=mock(HealthController.class);when(controller.health()).thenReturn(ResponseEntity.ok(Map.of("trails",Map.of("oportunidades",a,"empresas",b,"capacita",c))));assertEquals(expected,new TrailReadiness(controller).health().getStatus().getCode());}
}
