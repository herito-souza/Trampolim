package com.trampolim.api.career;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.trampolim.api.radar.SkillCatalog;
import com.trampolim.api.security.PlatformAccess;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

class CareerMentorshipServiceTest {
    private final JdbcTemplate db=mock(JdbcTemplate.class);
    private final SkillCatalog users=mock(SkillCatalog.class);
    private final PlatformAccess admins=mock(PlatformAccess.class);
    private final CareerMentorshipService service=new CareerMentorshipService(db,users,admins);

    @Test void mentorApplicationRequiresAdultAttestation() {
        when(users.user("candidate@example.test")).thenReturn(21L);
        var failure=assertThrows(ResponseStatusException.class,()->service.saveMentor("candidate@example.test",
                new CareerMentorshipService.MentorInput("Pessoa","Resumo","Experiência",java.util.List.of("Educação"),false,false)));
        assertEquals(400,failure.getStatusCode().value());
        verifyNoInteractions(db);
    }

    @Test void mentorApplicationRequiresExplicitPublicProfileConsent() {
        when(users.user("candidate@example.test")).thenReturn(21L);
        var failure=assertThrows(ResponseStatusException.class,()->service.saveMentor("candidate@example.test",
                new CareerMentorshipService.MentorInput("Pessoa","Resumo","Experiência",java.util.List.of("Educação"),true,false)));
        assertEquals(400,failure.getStatusCode().value());
        verifyNoInteractions(db);
    }

    @Test void privateModerationQueueRequiresPlatformAdmin() {
        when(admins.isSuperAdmin("user@example.test")).thenReturn(false);
        var failure=assertThrows(ResponseStatusException.class,()->service.adminMentors("user@example.test"));
        assertEquals(403,failure.getStatusCode().value());
        verifyNoInteractions(db,users);
    }
}
