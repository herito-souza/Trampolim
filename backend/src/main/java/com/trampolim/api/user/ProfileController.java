package com.trampolim.api.user;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Base64;
import com.trampolim.api.catalog.JobApplicationRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProfileController {
    private final AppUserRepository users;
    private final ExperienceRepository experiences;
    private final EducationRepository educations;
    private final ObjectMapper objectMapper;
    private final JobApplicationRepository applications;
    private final PasswordEncoder passwordEncoder;

    public record ProfileData(String name, String email, String phone, String location, String summary, String resumeFileName, String photoFileName) {}
    public record ProfileUpdate(String name, String phone, String location, String summary) {}
    public record ExperienceData(Long id, String position, String company, String startDate, String endDate, boolean current, String description) {}
    public record ExperienceInput(String position, String company, String startDate, String endDate, boolean current, String description) {}
    public record EducationData(Long id, String institution, String course, String level, String startDate, String endDate, boolean inProgress) {}
    public record EducationInput(String institution, String course, String level, String startDate, String endDate, boolean inProgress) {}
    public record ProfileResponse(ProfileData profile, List<ExperienceData> experiences, List<EducationData> educations) {}
    public record DiagnosticData(String trackId, String trackName, List<Integer> answers, Map<String, Integer> scores) {}
    public record FavoritesInput(List<String> ids) {}
    public record DeleteAccountInput(String password) {}
    public record AlertPreferences(boolean enabled, List<String> areas, String frequency, String city, String modality,
                                   String contractType, String workSchedule, String keywords, boolean useDistance,
                                   Double latitude, Double longitude, Integer radiusKm) {}
    public record PublicSettings(boolean enabled, boolean showLocation, boolean showSummary, boolean showExperiences, boolean showEducation) {}
    public record PublicSettingsData(String shareId, boolean enabled, boolean showLocation, boolean showSummary, boolean showExperiences, boolean showEducation) {}
    public record PublicProfile(String name, String location, String summary, List<ExperienceData> experiences, List<EducationData> educations, boolean summaryShared, boolean experiencesShared, boolean educationShared) {}

    @GetMapping("/profile/alerts")
    public AlertPreferences getAlertPreferences(@AuthenticationPrincipal String email) throws JsonProcessingException {
        String data = currentUser(email).getAlertPreferences();
        return data == null ? new AlertPreferences(false, List.of(), "weekly", "", "", "", "", "", false, null, null, null) : objectMapper.readValue(data, AlertPreferences.class);
    }

    @PutMapping("/profile/alerts")
    public AlertPreferences saveAlertPreferences(@AuthenticationPrincipal String email, @RequestBody AlertPreferences input) throws JsonProcessingException {
        List<String> validAreas = List.of("service", "sales", "admin", "numbers", "logistics", "maintenance", "production", "technology", "health", "marketing");
        String frequency = List.of("immediate", "daily", "weekly").contains(input.frequency()) ? input.frequency() : "weekly";
        List<String> areas = input.areas() == null ? List.of() : input.areas().stream().filter(validAreas::contains).distinct().toList();
        boolean validDistance = input.enabled() && input.useDistance() && input.latitude() != null && input.longitude() != null && input.radiusKm() != null
                && input.latitude() >= -90 && input.latitude() <= 90 && input.longitude() >= -180 && input.longitude() <= 180
                && List.of(5, 10, 25, 50).contains(input.radiusKm());
        if (input.useDistance() && !validDistance) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Permita a localização e escolha um raio válido para ativar alertas por distância.");
        AlertPreferences preferences = new AlertPreferences(input.enabled(), areas, frequency, clean(input.city(), 120),
                validOrEmpty(input.modality(), List.of("Presencial", "Híbrido", "Remoto")),
                validOrEmpty(input.contractType(), List.of("CLT", "PJ", "Temporário", "Estágio", "Aprendiz", "Autônomo")),
                clean(input.workSchedule(), 80), clean(input.keywords(), 160), validDistance,
                validDistance ? input.latitude() : null, validDistance ? input.longitude() : null, validDistance ? input.radiusKm() : null);
        AppUser user = currentUser(email);
        user.setAlertPreferences(objectMapper.writeValueAsString(preferences));
        users.save(user);
        return preferences;
    }

    private String clean(String value, int maxLength) {
        if (value == null || value.isBlank()) return "";
        String normalized = value.trim();
        return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized;
    }
    private String validOrEmpty(String value, List<String> allowed) {
        return value == null || value.isBlank() || !allowed.contains(value) ? "" : value;
    }

    @PutMapping("/profile/public")
    public Map<String, String> setPublicProfile(@AuthenticationPrincipal String email, @RequestBody PublicSettings settings) {
        AppUser user = currentUser(email);
        if (user.getPublicShareId() == null) user.setPublicShareId(java.util.UUID.randomUUID().toString());
        user.setPublicProfileEnabled(settings.enabled());
        user.setShareLocation(settings.showLocation());
        user.setShareSummary(settings.showSummary()); user.setShareExperiences(settings.showExperiences()); user.setShareEducation(settings.showEducation());
        users.save(user);
        return Map.of("shareId", user.getPublicShareId(), "enabled", Boolean.toString(user.isPublicProfileEnabled()));
    }

    @GetMapping("/profile/public")
    public PublicSettingsData getPublicSettings(@AuthenticationPrincipal String email) {
        AppUser user = currentUser(email);
        return new PublicSettingsData(user.getPublicShareId(), user.isPublicProfileEnabled(), user.isShareLocation(), user.isShareSummary(), user.isShareExperiences(), user.isShareEducation());
    }

    @GetMapping("/public/profiles/{shareId}")
    public PublicProfile getPublicProfile(@PathVariable String shareId) {
        AppUser user = users.findByPublicShareIdAndPublicProfileEnabledTrue(shareId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Este perfil não está disponível."));
        return new PublicProfile(user.getFullName(), user.isShareLocation() ? user.getLocation() : null, user.isShareSummary() ? user.getSummary() : null,
                user.isShareExperiences() ? experiences.findAllByUserIdOrderByIdDesc(user.getId()).stream().map(this::toData).toList() : List.of(),
                user.isShareEducation() ? educations.findAllByUserIdOrderByIdDesc(user.getId()).stream().map(this::toData).toList() : List.of(),
                user.isShareSummary(), user.isShareExperiences(), user.isShareEducation());
    }

    @GetMapping("/profile/favorites")
    public List<String> getFavorites(@AuthenticationPrincipal String email) throws JsonProcessingException {
        String data = currentUser(email).getFavoriteOpportunityIds();
        return data == null ? List.of() : objectMapper.readValue(data, new TypeReference<>() {});
    }

    @PutMapping("/profile/favorites")
    public List<String> saveFavorites(@AuthenticationPrincipal String email, @RequestBody FavoritesInput input) throws JsonProcessingException {
        List<String> ids = input.ids() == null ? List.of() : new ArrayList<>(new LinkedHashSet<>(input.ids()));
        if (ids.size() > 100 || ids.stream().anyMatch(id -> id == null || id.isBlank() || id.length() > 100)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A lista de favoritos é inválida.");
        }
        AppUser user = currentUser(email);
        user.setFavoriteOpportunityIds(objectMapper.writeValueAsString(ids));
        users.save(user);
        return ids;
    }

    @GetMapping("/profile/diagnostic")
    public ResponseEntity<DiagnosticData> getDiagnostic(@AuthenticationPrincipal String email) throws JsonProcessingException {
        String data = currentUser(email).getDiagnosticData();
        return data == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(objectMapper.readValue(data, DiagnosticData.class));
    }

    @PutMapping("/profile/diagnostic")
    public DiagnosticData saveDiagnostic(@AuthenticationPrincipal String email, @RequestBody DiagnosticData input) throws JsonProcessingException {
        if (input.answers() == null || input.answers().size() != 10 || input.scores() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O diagnóstico deve conter as respostas das dez perguntas.");
        }
        AppUser user = currentUser(email);
        user.setDiagnosticData(objectMapper.writeValueAsString(input));
        users.save(user);
        return input;
    }

    @GetMapping("/profile")
    public ProfileResponse getProfile(@AuthenticationPrincipal String email) {
        AppUser user = currentUser(email);
        return responseFor(user);
    }

    @GetMapping("/profile/export")
    public Map<String, Object> exportData(@AuthenticationPrincipal String email) throws JsonProcessingException {
        AppUser user = currentUser(email);
        Map<String, Object> export = new java.util.LinkedHashMap<>();
        export.put("exportedAt", java.time.Instant.now()); export.put("profile", responseFor(user));
        export.put("diagnostic", user.getDiagnosticData() == null ? null : objectMapper.readTree(user.getDiagnosticData()));
        export.put("alertPreferences", user.getAlertPreferences() == null ? null : objectMapper.readTree(user.getAlertPreferences()));
        export.put("favoriteOpportunityIds", user.getFavoriteOpportunityIds() == null ? List.of() : objectMapper.readValue(user.getFavoriteOpportunityIds(), new TypeReference<>() {}));
        export.put("resume", user.getResumeContent() == null ? null : Map.of("fileName", user.getResumeFileName(), "contentType", user.getResumeContentType(), "base64", Base64.getEncoder().encodeToString(user.getResumeContent())));
        export.put("photo", user.getPhotoContent() == null ? null : Map.of("fileName", user.getPhotoFileName(), "contentType", user.getPhotoContentType(), "base64", Base64.getEncoder().encodeToString(user.getPhotoContent())));
        export.put("applications", applications.findAllByUserIdOrderByUpdatedAtDesc(user.getId()).stream().map(application -> Map.of(
                "title", application.getItem().getTitle(), "provider", application.getItem().getProvider(), "status", application.getStatus(),
                "notes", application.getNotes() == null ? "" : application.getNotes(), "followUpDate", application.getFollowUpDate() == null ? "" : application.getFollowUpDate().toString())).toList());
        return export;
    }

    @DeleteMapping("/profile")
    @Transactional
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal String email, @RequestBody DeleteAccountInput input) {
        AppUser user = currentUser(email);
        if (input == null || input.password() == null || !passwordEncoder.matches(input.password(), user.getPasswordHash()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Confirme sua senha para excluir a conta.");
        Long id = user.getId();
        applications.deleteAllByUserId(id);
        experiences.deleteAllByUserId(id);
        educations.deleteAllByUserId(id);
        users.delete(user);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/profile")
    public ProfileResponse updateProfile(@AuthenticationPrincipal String email, @RequestBody ProfileUpdate input) {
        AppUser user = currentUser(email);
        String name = input.name() == null ? "" : input.name().trim();
        if (name.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O nome é obrigatório.");
        user.setFullName(name);
        user.setPhone(clean(input.phone()));
        user.setLocation(clean(input.location()));
        user.setSummary(clean(input.summary()));
        return responseFor(users.save(user));
    }

    @PostMapping("/profile/experiences")
    public ExperienceData addExperience(@AuthenticationPrincipal String email, @RequestBody ExperienceInput input) {
        AppUser user = currentUser(email);
        Experience item = new Experience();
        item.setUser(user);
        item.setPosition(required(input.position(), "O cargo é obrigatório."));
        item.setCompany(required(input.company(), "A empresa é obrigatória."));
        item.setStartDate(clean(input.startDate()));
        item.setEndDate(input.current() ? null : clean(input.endDate()));
        item.setCurrent(input.current());
        item.setDescription(clean(input.description()));
        return toData(experiences.save(item));
    }

    @DeleteMapping("/profile/experiences/{id}")
    public ResponseEntity<Void> removeExperience(@AuthenticationPrincipal String email, @PathVariable Long id) {
        Experience item = experiences.findByIdAndUserId(id, currentUser(email).getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        experiences.delete(item);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/profile/educations")
    public EducationData addEducation(@AuthenticationPrincipal String email, @RequestBody EducationInput input) {
        AppUser user = currentUser(email);
        Education item = new Education();
        item.setUser(user);
        item.setInstitution(required(input.institution(), "A instituição é obrigatória."));
        item.setCourse(required(input.course(), "O curso é obrigatório."));
        item.setLevel(clean(input.level()));
        item.setStartDate(clean(input.startDate()));
        item.setEndDate(input.inProgress() ? null : clean(input.endDate()));
        item.setInProgress(input.inProgress());
        return toData(educations.save(item));
    }

    @DeleteMapping("/profile/educations/{id}")
    public ResponseEntity<Void> removeEducation(@AuthenticationPrincipal String email, @PathVariable Long id) {
        Education item = educations.findByIdAndUserId(id, currentUser(email).getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        educations.delete(item);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/profile/resume")
    public ProfileData uploadResume(@AuthenticationPrincipal String email, @RequestParam("file") MultipartFile file) throws IOException {
        AppUser user = currentUser(email);
        if (file.isEmpty() || file.getSize() > 10 * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escolha um arquivo PDF de até 10 MB.");
        }
        byte[] content = file.getBytes();
        String originalName = safeUploadName(file.getOriginalFilename(), "curriculo.pdf");
        if (!originalName.toLowerCase().endsWith(".pdf") || content.length < 5 ||
                content[0] != '%' || content[1] != 'P' || content[2] != 'D' || content[3] != 'F' || content[4] != '-') {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O currículo deve estar em formato PDF.");
        }
        user.setResumeFileName(originalName);
        user.setResumeContentType(MediaType.APPLICATION_PDF_VALUE);
        user.setResumeContent(content);
        return toProfile(users.save(user));
    }

    @GetMapping("/profile/resume")
    public ResponseEntity<byte[]> downloadResume(@AuthenticationPrincipal String email) {
        AppUser user = currentUser(email);
        if (user.getResumeContent() == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(safeUploadName(user.getResumeFileName(), "curriculo.pdf")).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store, max-age=0")
                .header("X-Content-Type-Options", "nosniff")
                .body(user.getResumeContent());
    }

    @DeleteMapping("/profile/resume")
    public ResponseEntity<Void> deleteResume(@AuthenticationPrincipal String email) {
        AppUser user = currentUser(email);
        user.setResumeFileName(null);
        user.setResumeContentType(null);
        user.setResumeContent(null);
        users.save(user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/profile/photo")
    public ProfileData uploadPhoto(@AuthenticationPrincipal String email, @RequestParam("file") MultipartFile file) throws IOException {
        AppUser user = currentUser(email);
        String contentType = file.getContentType();
        if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024 || contentType == null ||
                !List.of("image/jpeg", "image/png", "image/webp").contains(contentType.toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escolha uma imagem JPG, PNG ou WEBP de até 5 MB.");
        }
        byte[] content = file.getBytes();
        if (!matchesImageSignature(content, contentType.toLowerCase())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O conteúdo do arquivo não corresponde a uma imagem JPG, PNG ou WEBP válida.");
        user.setPhotoFileName(safeUploadName(file.getOriginalFilename(), "foto-perfil"));
        user.setPhotoContentType(contentType.toLowerCase());
        user.setPhotoContent(content);
        return toProfile(users.save(user));
    }

    @GetMapping("/profile/photo")
    public ResponseEntity<byte[]> getPhoto(@AuthenticationPrincipal String email) {
        AppUser user = currentUser(email);
        if (user.getPhotoContent() == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(user.getPhotoContentType()))
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store, max-age=0")
                .header("X-Content-Type-Options", "nosniff")
                .body(user.getPhotoContent());
    }

    @DeleteMapping("/profile/photo")
    public ResponseEntity<Void> deletePhoto(@AuthenticationPrincipal String email) {
        AppUser user = currentUser(email);
        user.setPhotoFileName(null);
        user.setPhotoContentType(null);
        user.setPhotoContent(null);
        users.save(user);
        return ResponseEntity.noContent().build();
    }

    private AppUser currentUser(String email) {
        return users.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private String safeUploadName(String filename, String fallback) {
        if (filename == null || filename.isBlank()) return fallback;
        String normalized = filename.replace('\\', '/');
        String basename = normalized.substring(normalized.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}\"]", "_").trim();
        if (basename.isBlank() || basename.equals(".") || basename.equals("..")) return fallback;
        return basename.length() > 180 ? basename.substring(basename.length() - 180) : basename;
    }

    private boolean matchesImageSignature(byte[] data, String contentType) {
        if ("image/png".equals(contentType)) return data.length >= 8 && (data[0] & 0xff) == 0x89 && data[1] == 'P' && data[2] == 'N' && data[3] == 'G';
        if ("image/jpeg".equals(contentType)) return data.length >= 3 && (data[0] & 0xff) == 0xff && (data[1] & 0xff) == 0xd8 && (data[2] & 0xff) == 0xff;
        return "image/webp".equals(contentType) && data.length >= 12 && data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F'
                && data[8] == 'W' && data[9] == 'E' && data[10] == 'B' && data[11] == 'P';
    }

    private ProfileResponse responseFor(AppUser user) {
        return new ProfileResponse(toProfile(user), experiences.findAllByUserIdOrderByIdDesc(user.getId()).stream().map(this::toData).toList(),
                educations.findAllByUserIdOrderByIdDesc(user.getId()).stream().map(this::toData).toList());
    }

    private ProfileData toProfile(AppUser user) {
        return new ProfileData(user.getFullName(), user.getEmail(), user.getPhone(), user.getLocation(), user.getSummary(), user.getResumeFileName(), user.getPhotoFileName());
    }
    private ExperienceData toData(Experience item) {
        return new ExperienceData(item.getId(), item.getPosition(), item.getCompany(), item.getStartDate(), item.getEndDate(), item.isCurrent(), item.getDescription());
    }
    private EducationData toData(Education item) {
        return new EducationData(item.getId(), item.getInstitution(), item.getCourse(), item.getLevel(), item.getStartDate(), item.getEndDate(), item.isInProgress());
    }
    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String required(String value, String message) {
        String cleaned = clean(value);
        if (cleaned == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        return cleaned;
    }
}
