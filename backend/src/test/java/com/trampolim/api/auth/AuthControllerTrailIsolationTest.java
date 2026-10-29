package com.trampolim.api.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.trampolim.api.security.TrailTokenService;
import com.trampolim.api.user.AppUser;
import com.trampolim.api.user.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;

class AuthControllerTrailIsolationTest {
    @Test
    void legacyCompanyMembershipDoesNotChangeAnOpportunitiesLoginIdentity() {
        AppUserRepository users = mock(AppUserRepository.class);
        PasswordEncoder passwords = mock(PasswordEncoder.class);
        TrailTokenService tokens = new TrailTokenService();
        ReflectionTestUtils.setField(tokens, "oportunidadesSecret", "opportunities-test-secret-0123456789-abcdefghijklmnopqrstuvwxyz");
        @SuppressWarnings("unchecked") ObjectProvider<JavaMailSender> mail = mock(ObjectProvider.class);
        JdbcTemplate legacyDatabase = mock(JdbcTemplate.class);
        when(legacyDatabase.queryForObject("SELECT COUNT(*) FROM company_members WHERE user_id=?", Integer.class, 19L)).thenReturn(1);
        AuthController controller = new AuthController(users, passwords, tokens, mail, legacyDatabase);
        AppUser user = new AppUser();
        user.setId(19L);
        user.setEmail("person@example.com");
        user.setFullName("Pessoa Exemplo");
        user.setAccountType("PERSON");

        AuthController.AuthResponse response = ReflectionTestUtils.invokeMethod(controller, "responseFor", user);

        assertEquals("PERSON", response.accountType());
    }
}
