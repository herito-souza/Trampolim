package com.trampolim.api.auth;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.trampolim.api.user.AppUserRepository;
import com.trampolim.api.security.TrailTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
class RegistrationBoundsTest {
 @Test @SuppressWarnings("unchecked") void rejectsOversizedNamesAndEmailsBeforePersistenceOrHashing(){var users=mock(AppUserRepository.class);var encoder=mock(PasswordEncoder.class);var controller=new AuthController(users,encoder,mock(TrailTokenService.class),(ObjectProvider<JavaMailSender>)mock(ObjectProvider.class),mock(JdbcTemplate.class));for(var input:java.util.List.of(new AuthController.Registration("x".repeat(121),"valid@example.test","Valid-password!",true),new AuthController.Registration("Valid","x".repeat(180)+"@example.test","Valid-password!",true))){assertEquals(400,assertThrows(ResponseStatusException.class,()->controller.register(input,new MockHttpServletRequest())).getStatusCode().value());}verifyNoInteractions(users,encoder);}
}
