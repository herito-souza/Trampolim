package com.trampolim.api.criticalflows;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trampolim.api.auth.AuthController;
import com.trampolim.api.config.ClientErrorController;
import com.trampolim.api.catalog.*;
import com.trampolim.api.security.TrailTokenService;
import com.trampolim.api.security.TrailTokenService.Trail;
import com.trampolim.api.user.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

class CriticalFlowsTest {
    private MockHttpServletRequest request() {
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        return request;
    }

    @Test
    @SuppressWarnings("unchecked")
    void loginNormalizesEmailAndReturnsSessionToken() {
        AppUserRepository users = mock(AppUserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        TrailTokenService tokens = mock(TrailTokenService.class);
        ObjectProvider<JavaMailSender> mail = mock(ObjectProvider.class);
        AppUser user = new AppUser(); user.setId(7L); user.setEmail("person@example.com"); user.setFullName("Pessoa"); user.setPasswordHash("hash");
        when(users.findByEmail("person@example.com")).thenReturn(Optional.of(user));
        when(encoder.matches("senha-segura", "hash")).thenReturn(true);
        when(tokens.issue(Trail.OPORTUNIDADES, "person@example.com", 7L, 0)).thenReturn("session-token");

        AuthController controller = new AuthController(users, encoder, tokens, mail, mock(org.springframework.jdbc.core.JdbcTemplate.class));
        var response = controller.login(new AuthController.Credentials(" PERSON@EXAMPLE.COM ", "senha-segura"), request());

        assertEquals("session-token", response.token());
        assertEquals("person@example.com", response.user().email());
        verify(users).save(user);
    }

    @Test
    void favoritesAreDeduplicatedAndPersisted() throws Exception {
        AppUserRepository users = mock(AppUserRepository.class);
        AppUser user = new AppUser(); user.setId(3L); user.setEmail("person@example.com");
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        ProfileController controller = new ProfileController(users, mock(ExperienceRepository.class), mock(EducationRepository.class), new ObjectMapper(), mock(JobApplicationRepository.class), mock(PasswordEncoder.class));

        var favorites = controller.saveFavorites(user.getEmail(), new ProfileController.FavoritesInput(List.of("course-a", "course-a", "job-b")));

        assertEquals(List.of("course-a", "job-b"), favorites);
        assertEquals("[\"course-a\",\"job-b\"]", user.getFavoriteOpportunityIds());
        verify(users).save(user);
    }

    @Test
    void applicationTrackingCreatesSavedApplicationForActiveJob() {
        JobApplicationRepository applications = mock(JobApplicationRepository.class);
        AppUserRepository users = mock(AppUserRepository.class);
        CatalogItemRepository items = mock(CatalogItemRepository.class);
        AppUser user = new AppUser(); user.setId(3L); user.setEmail("person@example.com");
        CatalogItem job = new CatalogItem(); job.setId(11L); job.setKind("job"); job.setTitle("Atendente"); job.setProvider("Loja"); job.setActive(true);
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(items.findById(11L)).thenReturn(Optional.of(job));
        when(applications.findByUserIdAndItemId(3L, 11L)).thenReturn(Optional.empty());
        when(applications.save(any(JobApplication.class))).thenAnswer(call -> call.getArgument(0));
        JobApplicationController controller = new JobApplicationController(applications, users, items);

        var result = controller.add(user.getEmail(), new JobApplicationController.ApplicationInput(11L));

        assertEquals(11L, result.itemId());
        assertEquals("Atendente", result.title());
        assertEquals("SAVED", result.status());
        verify(applications).save(any(JobApplication.class));
    }

    @Test
    void publicCatalogOmitsExpiredAndUnapprovedItems() {
        CatalogItemRepository items = mock(CatalogItemRepository.class);
        CatalogItem active = item(1L, true, "APPROVED", LocalDate.now().plusDays(3));
        CatalogItem expired = item(2L, true, "APPROVED", LocalDate.now().minusDays(1));
        CatalogItem pending = item(3L, false, "PENDING", null);
        when(items.findAllByKindAndActiveTrueOrderByCreatedAtDesc("job")).thenReturn(List.of(active, expired, pending));
        CatalogController controller = new CatalogController(items, mock(JobApplicationRepository.class), mock(OpportunityAlertService.class), mock(com.trampolim.api.security.PlatformAccess.class));

        var result = controller.list("job");

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).id());
        assertEquals("UNKNOWN", result.get(0).linkStatus());
    }

    @Test
    void applicationTrackingRejectsInactiveJobs() {
        JobApplicationRepository applications = mock(JobApplicationRepository.class);
        AppUserRepository users = mock(AppUserRepository.class);
        CatalogItemRepository items = mock(CatalogItemRepository.class);
        AppUser user = new AppUser(); user.setId(3L); user.setEmail("person@example.com");
        CatalogItem job = item(11L, false, "APPROVED", null); job.setKind("job");
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(items.findById(11L)).thenReturn(Optional.of(job));
        var controller = new JobApplicationController(applications, users, items);

        assertThrows(ResponseStatusException.class, () -> controller.add(user.getEmail(), new JobApplicationController.ApplicationInput(11L)));
        verify(applications, never()).save(any());
    }

    @Test
    void communityReportIsStoredWithoutExposingReporterIdentity() {
        CatalogReportRepository reports = mock(CatalogReportRepository.class);
        CatalogItemRepository items = mock(CatalogItemRepository.class);
        AppUserRepository users = mock(AppUserRepository.class);
        AppUser user = new AppUser(); user.setId(4L); user.setEmail("person@example.com");
        CatalogItem job = item(12L, true, "APPROVED", null);
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(items.findById(12L)).thenReturn(Optional.of(job));
        when(reports.existsByItem_IdAndReporter_IdAndCreatedAtAfter(eq(12L), eq(4L), any())).thenReturn(false);
        when(reports.save(any(CatalogReport.class))).thenAnswer(call -> call.getArgument(0));
        var controller = new CatalogReportController(reports, items, users, mock(com.trampolim.api.security.PlatformAccess.class));

        controller.report(user.getEmail(), "job", 12L, new CatalogReportController.ReportInput("BROKEN_LINK", "   Link dá erro   "));

        var saved = org.mockito.ArgumentCaptor.forClass(CatalogReport.class);
        verify(reports).save(saved.capture());
        assertEquals("Link dá erro", saved.getValue().getDetails());
        assertSame(user, saved.getValue().getReporter());
    }

    @Test
    void publicMonitoringAcceptsOnlySafePathsAndKnownErrorKinds() {
        var controller = new ClientErrorController();
        var request = request();
        assertThrows(ResponseStatusException.class,
                () -> controller.report(new ClientErrorController.ClientErrorInput("runtime", "/perfil?email=private@example.com"), request));
        assertThrows(ResponseStatusException.class,
                () -> controller.report(new ClientErrorController.ClientErrorInput("arbitrary", "/"), request));
    }

    @Test
    void resumeUploadRejectsFilesWithPdfExtensionButWrongContents() throws Exception {
        AppUserRepository users = mock(AppUserRepository.class);
        AppUser user = new AppUser(); user.setId(4L); user.setEmail("person@example.com"); user.setFullName("Pessoa");
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        var controller = new ProfileController(users, mock(ExperienceRepository.class), mock(EducationRepository.class), new ObjectMapper(), mock(JobApplicationRepository.class), mock(PasswordEncoder.class));
        var fakePdf = new org.springframework.mock.web.MockMultipartFile("file", "../../script.pdf", "application/pdf", "not a pdf".getBytes());

        assertThrows(ResponseStatusException.class, () -> controller.uploadResume(user.getEmail(), fakePdf));
        verify(users, never()).save(any(AppUser.class));
    }

    @Test
    void photoUploadRejectsMismatchedMimeTypeAndContent() throws Exception {
        AppUserRepository users = mock(AppUserRepository.class);
        AppUser user = new AppUser(); user.setId(4L); user.setEmail("person@example.com"); user.setFullName("Pessoa");
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        var controller = new ProfileController(users, mock(ExperienceRepository.class), mock(EducationRepository.class), new ObjectMapper(), mock(JobApplicationRepository.class), mock(PasswordEncoder.class));
        var fakeImage = new org.springframework.mock.web.MockMultipartFile("file", "profile.png", "image/png", "not an image".getBytes());

        assertThrows(ResponseStatusException.class, () -> controller.uploadPhoto(user.getEmail(), fakeImage));
        verify(users, never()).save(any(AppUser.class));
    }

    private CatalogItem item(Long id, boolean active, String status, LocalDate expiresAt) {
        CatalogItem item = new CatalogItem(); item.setId(id); item.setKind("job"); item.setTitle("Vaga " + id); item.setProvider("Empresa");
        item.setDescription("Descrição"); item.setUrl("https://example.com"); item.setArea("service"); item.setActive(active);
        item.setModerationStatus(status); item.setExpiresAt(expiresAt); return item;
    }
}
