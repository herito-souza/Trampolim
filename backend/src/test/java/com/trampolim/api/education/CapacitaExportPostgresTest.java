package com.trampolim.api.education;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import com.trampolim.api.BackendApplication;

@SpringBootTest(classes = BackendApplication.class)
class CapacitaExportPostgresTest {
    @Autowired CapacitaController controller;
    @Autowired com.trampolim.api.auth.IndependentTrailAuthController authentication;
    @Autowired StudentAccountController accounts;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder passwords;
    @Autowired @Qualifier("capacitaJdbcTemplate") JdbcTemplate students;

    @Test
    @Transactional(transactionManager = "capacitaTransactionManager")
    void eachExportContainsOnlyTheSelectedStudentAndNoCredentialColumns() {
        String first = "export-" + UUID.randomUUID() + "@example.test";
        String second = "export-" + UUID.randomUUID() + "@example.test";
        for (String email : List.of(first, second)) {
            students.update("INSERT INTO student_users(email,password_hash,full_name,education_stage,institution_name,email_verified,account_status,terms_accepted_at) VALUES (?,'test-hash','Export Test','HIGH_SCHOOL','Test school',TRUE,'ACTIVE',CURRENT_TIMESTAMP)", email);
        }
        Long categoryId = students.queryForObject("SELECT id FROM education_categories ORDER BY id LIMIT 1", Long.class);
        Long firstContent = students.queryForObject("INSERT INTO education_contents(category_id,title,summary,status) VALUES (?,'Export fixture one','Demonstration for automated test','DRAFT') RETURNING id", Long.class, categoryId);
        Long secondContent = students.queryForObject("INSERT INTO education_contents(category_id,title,summary,status) VALUES (?,'Export fixture two','Demonstration for automated test','DRAFT') RETURNING id", Long.class, categoryId);
        students.update("INSERT INTO student_education_progress(student_user_id,content_id) SELECT id,? FROM student_users WHERE email=?", firstContent, first);
        students.update("INSERT INTO student_education_progress(student_user_id,content_id) SELECT id,? FROM student_users WHERE email=?", secondContent, second);
        assertNull(accounts.ageDeclaration(first).ageBand());
        assertNull(accounts.ageDeclaration(second).ageBand());
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> accounts.declareAge(first,new StudentAccountController.AgeDeclaration("UNDER_14")));
        accounts.declareAge(first,new StudentAccountController.AgeDeclaration("AGE_14_17"));
        assertEquals("AGE_14_17",accounts.ageDeclaration(first).ageBand());
        assertNull(accounts.ageDeclaration(second).ageBand());
        accounts.declareAge(first,new StudentAccountController.AgeDeclaration("AGE_18_PLUS"));
        assertEquals("AGE_18_PLUS",accounts.ageDeclaration(first).ageBand());
        var result = controller.exportAccount(first);
        assertEquals("capacita", result.get("trail"));
        Map<?, ?> account = (Map<?, ?>) result.get("account");
        assertEquals(first, account.get("email"));
        assertEquals(java.util.Set.of("id", "email", "full_name", "education_stage", "institution_name", "email_verified", "terms_accepted_at", "created_at", "age_band", "age_declared_at"), account.keySet());
        assertFalse(account.containsValue(second));
        List<?> progress = (List<?>) result.get("progress");
        assertEquals(1, progress.size());
        Map<?, ?> row = (Map<?, ?>) progress.get(0);
        assertEquals(firstContent, row.get("content_id"));
        assertEquals(java.util.Set.of("content_id", "completed_at"), row.keySet());
        List<?> secondProgress = (List<?>) controller.exportAccount(second).get("progress");
        assertEquals(secondContent, ((Map<?, ?>) secondProgress.get(0)).get("content_id"));
        assertEquals(second, ((Map<?, ?>) controller.exportAccount(second).get("account")).get("email"));
        var access = org.mockito.Mockito.mock(com.trampolim.api.security.PlatformAccess.class);
        org.mockito.Mockito.when(access.isSuperAdmin("admin@example.test")).thenReturn(true);
        var admin = new com.trampolim.api.organization.TrailAccountAdminController(access, provider(null), provider(students));
        long secondId = students.queryForObject("SELECT id FROM student_users WHERE email=?",Long.class,second);
        var tokens = new com.trampolim.api.security.TrailTokenService();
        org.springframework.test.util.ReflectionTestUtils.setField(tokens,"capacitaSecret","student-token-test-secret-abcdefghijklmnopqrstuvwxyz-2026");
        String oldToken = tokens.issue(com.trampolim.api.security.TrailTokenService.Trail.CAPACITA,second,secondId,0);
        var filter = new com.trampolim.api.security.JwtAuthenticationFilter(org.mockito.Mockito.mock(com.trampolim.api.security.JwtUtil.class),org.mockito.Mockito.mock(com.trampolim.api.user.AppUserRepository.class),tokens,provider(null),provider(students));
        assertStudentToken(filter,oldToken,true);
        admin.changeStatus("admin@example.test","capacita",secondId,new com.trampolim.api.organization.TrailAccountAdminController.StatusChange("SUSPENDED","Test suspension"));
        assertStudentToken(filter,oldToken,false);
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> controller.exportAccount(second));
        assertEquals("ACTIVE",students.queryForObject("SELECT account_status FROM student_users WHERE email=?",String.class,first));
        admin.changeStatus("admin@example.test","capacita",secondId,new com.trampolim.api.organization.TrailAccountAdminController.StatusChange("ACTIVE","Test reactivation"));
        assertEquals(2,students.queryForObject("SELECT token_version FROM student_users WHERE id=?",Integer.class,secondId));
        assertStudentToken(filter,oldToken,false);
        assertStudentToken(filter,tokens.issue(com.trampolim.api.security.TrailTokenService.Trail.CAPACITA,second,secondId,2),true);
        assertEquals(2L,students.queryForObject("SELECT count(*) FROM account_admin_audit WHERE target_user_id=?",Long.class,secondId));
        assertEquals(second,((Map<?,?>) controller.exportAccount(second).get("account")).get("email"));
        students.update("UPDATE student_users SET password_hash=? WHERE email=?", passwords.encode("current-password-2026"), first);
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> accounts.deleteAccount(first, new StudentAccountController.DeleteAccount("wrong-password")));
        assertEquals(1L, students.queryForObject("SELECT count(*) FROM student_users WHERE email=?", Long.class, first));
        accounts.deleteAccount(first, new StudentAccountController.DeleteAccount("current-password-2026"));
        assertEquals(0L, students.queryForObject("SELECT count(*) FROM student_users WHERE email=?", Long.class, first));
        assertEquals(0L, students.queryForObject("SELECT count(*) FROM student_education_progress WHERE student_user_id=?", Long.class, account.get("id")));
        assertEquals(1L, students.queryForObject("SELECT count(*) FROM student_users WHERE email=?", Long.class, second));
        assertEquals(secondContent, ((Map<?, ?>) ((List<?>) controller.exportAccount(second).get("progress")).get(0)).get("content_id"));
    }
    @SuppressWarnings("unchecked")
    private <T> org.springframework.beans.factory.ObjectProvider<T> provider(T value) {
        var provider = org.mockito.Mockito.mock(org.springframework.beans.factory.ObjectProvider.class);
        org.mockito.Mockito.when(provider.getIfAvailable()).thenReturn(value);
        return provider;
    }
    private void assertStudentToken(com.trampolim.api.security.JwtAuthenticationFilter filter,String token,boolean accepted) {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        try {
            var request = new org.springframework.mock.web.MockHttpServletRequest("GET","/api/capacita/progress");
            request.addHeader("Authorization","Bearer "+token);
            filter.doFilter(request,new org.springframework.mock.web.MockHttpServletResponse(),(req,res)->{});
            assertEquals(accepted,org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication()!=null);
        } catch (Exception failure) { throw new AssertionError(failure); }
        finally { org.springframework.security.core.context.SecurityContextHolder.clearContext(); }
    }
    @Test @Transactional(transactionManager = "capacitaTransactionManager")
    void studentRegistrationRequiresExplicitSupportedAgeBand() {
        var http = new org.springframework.mock.web.MockHttpServletRequest("POST","/api/capacita/auth/register");
        http.setRemoteAddr("192.0.2.19");
        for (String age : List.of("AGE_14_17","AGE_18_PLUS")) {
            String email = "age-"+UUID.randomUUID()+"@example.test";
            authentication.registerStudent(new com.trampolim.api.auth.IndependentTrailAuthController.StudentRegistration("Student Test",email,"age-test-password","HIGH_SCHOOL","Test school",true,age),http);
            assertEquals(age,students.queryForObject("SELECT age_band FROM student_users WHERE email=?",String.class,email));
            assertNotNull(students.queryForObject("SELECT age_declared_at FROM student_users WHERE email=?",java.sql.Timestamp.class,email));
            assertEquals(age,((Map<?,?>) controller.exportAccount(email).get("account")).get("age_band"));
        }
        for (String invalid : List.of("","UNDER_14","UNKNOWN")) {
            String email = "age-invalid-"+UUID.randomUUID()+"@example.test";
            assertThrows(org.springframework.web.server.ResponseStatusException.class,()->authentication.registerStudent(new com.trampolim.api.auth.IndependentTrailAuthController.StudentRegistration("Student Test",email,"age-test-password","HIGH_SCHOOL","Test school",true,invalid),http));
            assertEquals(0L,students.queryForObject("SELECT count(*) FROM student_users WHERE email=?",Long.class,email));
        }
    }
    @Test @Transactional(transactionManager = "capacitaTransactionManager")
    void passwordRecoveryStoresOnlyHashAndConsumesLinkOnce() {
        String email="reset-"+UUID.randomUUID()+"@example.test";
        students.update("INSERT INTO student_users(email,password_hash,full_name,education_stage,email_verified,account_status,terms_accepted_at) VALUES (?,?,'Reset fixture','HIGH_SCHOOL',TRUE,'ACTIVE',CURRENT_TIMESTAMP)",email,passwords.encode("old-password-2026"));
        var mail=org.mockito.Mockito.mock(org.springframework.mail.javamail.JavaMailSender.class);
        var reset=new com.trampolim.api.auth.IndependentTrailAuthController(provider(null),provider(students),passwords,new com.trampolim.api.security.TrailTokenService(),provider(mail));
        org.springframework.test.util.ReflectionTestUtils.setField(reset,"mailEnabled",true);
        org.springframework.test.util.ReflectionTestUtils.setField(reset,"frontendUrl","https://example.test");
        var http=new org.springframework.mock.web.MockHttpServletRequest("POST","/api/capacita/auth/password/reset-request");
        http.setRemoteAddr("192.0.2.33");
        var response=reset.resetStudentRequest(new com.trampolim.api.auth.IndependentTrailAuthController.PasswordResetRequest(email),http);
        var message=org.mockito.ArgumentCaptor.forClass(org.springframework.mail.SimpleMailMessage.class);
        org.mockito.Mockito.verify(mail).send(message.capture());
        assertArrayEquals(new String[]{email},message.getValue().getTo());
        String text=message.getValue().getText();
        assertNotNull(text);
        assertTrue(text.contains("/#capacita/reset?token="));
        String token=text.substring(text.indexOf("?token=")+7);
        String stored=students.queryForObject("SELECT password_reset_token_hash FROM student_users WHERE email=?",String.class,email);
        assertNotEquals(token,stored);
        assertEquals(64,stored.length());
        assertTrue(students.queryForObject("SELECT password_reset_expires_at>CURRENT_TIMESTAMP AND password_reset_expires_at<=CURRENT_TIMESTAMP+INTERVAL '31 minutes' FROM student_users WHERE email=?",Boolean.class,email));
        reset.resetStudent(new com.trampolim.api.auth.IndependentTrailAuthController.PasswordReset(token,"new-password-2026"));
        assertTrue(passwords.matches("new-password-2026",students.queryForObject("SELECT password_hash FROM student_users WHERE email=?",String.class,email)));
        assertEquals(1,students.queryForObject("SELECT token_version FROM student_users WHERE email=?",Integer.class,email));
        assertNull(students.queryForObject("SELECT password_reset_token_hash FROM student_users WHERE email=?",String.class,email));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->reset.resetStudent(new com.trampolim.api.auth.IndependentTrailAuthController.PasswordReset(token,"another-password-2026")));
        assertEquals(response,reset.resetStudentRequest(new com.trampolim.api.auth.IndependentTrailAuthController.PasswordResetRequest("missing-"+UUID.randomUUID()+"@example.test"),http));
        org.mockito.Mockito.verifyNoMoreInteractions(mail);
    }
    @Test @Transactional(transactionManager = "capacitaTransactionManager")
    void studentEmailVerificationDoesNotIssueSessionBeforeConfirmation() {
        String email="verify-"+UUID.randomUUID()+"@example.test";
        var mail=org.mockito.Mockito.mock(org.springframework.mail.javamail.JavaMailSender.class);
        var tokens=new com.trampolim.api.security.TrailTokenService();
        var auth=new com.trampolim.api.auth.IndependentTrailAuthController(provider(null),provider(students),passwords,tokens,provider(mail));
        org.springframework.test.util.ReflectionTestUtils.setField(auth,"mailEnabled",true);
        org.springframework.test.util.ReflectionTestUtils.setField(auth,"frontendUrl","https://example.test");
        var http=new org.springframework.mock.web.MockHttpServletRequest("POST","/api/capacita/auth/register");
        http.setRemoteAddr("192.0.2.55");
        var response=auth.registerStudent(new com.trampolim.api.auth.IndependentTrailAuthController.StudentRegistration("Verification fixture",email,"verify-password-2026","HIGH_SCHOOL","Test school",true,"AGE_14_17"),http);
        assertFalse(response.emailVerified());
        assertEquals("",response.token());
        var message=org.mockito.ArgumentCaptor.forClass(org.springframework.mail.SimpleMailMessage.class);
        org.mockito.Mockito.verify(mail).send(message.capture());
        String text=message.getValue().getText();
        assertNotNull(text);
        assertTrue(text.contains("/#capacita/verify?token="));
        String token=text.substring(text.indexOf("?token=")+7);
        assertNotEquals(token,students.queryForObject("SELECT email_verification_token_hash FROM student_users WHERE email=?",String.class,email));
        assertTrue(students.queryForObject("SELECT email_verification_expires_at>CURRENT_TIMESTAMP FROM student_users WHERE email=?",Boolean.class,email));
        auth.verifyStudentEmail(token);
        assertTrue(students.queryForObject("SELECT email_verified FROM student_users WHERE email=?",Boolean.class,email));
        assertNull(students.queryForObject("SELECT email_verification_token_hash FROM student_users WHERE email=?",String.class,email));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->auth.verifyStudentEmail(token));
    }
}