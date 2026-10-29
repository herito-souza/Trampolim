package com.trampolim.api.organization;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.trampolim.api.security.PlatformAccess;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;
class TrailAccountAdminAuthorizationTest {
    @Test @SuppressWarnings("unchecked")
    void unauthorizedRequestsNeverResolveEitherPrivateDatabase() {
        var access=mock(PlatformAccess.class);
        ObjectProvider<JdbcTemplate> companies=mock(ObjectProvider.class);
        ObjectProvider<JdbcTemplate> students=mock(ObjectProvider.class);
        var controller=new TrailAccountAdminController(access,companies,students);
        assertEquals(403,assertThrows(ResponseStatusException.class,()->controller.list("ordinary@example.test","empresas",0,50)).getStatusCode().value());
        assertEquals(403,assertThrows(ResponseStatusException.class,()->controller.changeStatus("ordinary@example.test","capacita",1,new TrailAccountAdminController.StatusChange("SUSPENDED","Reason"))).getStatusCode().value());
        assertEquals(403,assertThrows(ResponseStatusException.class,()->controller.audit("ordinary@example.test","capacita",1,0,50)).getStatusCode().value());
        verifyNoInteractions(companies,students);
    }
}
