package com.trampolim.api.catalog;

import com.trampolim.api.user.AppUserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/profile/applications")
@RequiredArgsConstructor
public class JobApplicationController {
    private final JobApplicationRepository applications;
    private final AppUserRepository users;
    private final CatalogItemRepository items;
    public record ApplicationInput(Long itemId) {}
    public record StatusInput(String status) {}
    public record DetailsInput(String notes, LocalDate followUpDate) {}
    public record ApplicationData(Long id, Long itemId, String title, String provider, String location, String url, String status, Instant createdAt, Instant updatedAt, String notes, LocalDate followUpDate) {}
    private static final Set<String> STATUSES = Set.of("SAVED", "APPLIED", "INTERVIEW", "OFFER", "REJECTED");

    @GetMapping public List<ApplicationData> list(@AuthenticationPrincipal String email) {
        Long userId = users.findByEmail(email).orElseThrow().getId();
        return applications.findAllByUserIdOrderByUpdatedAtDesc(userId).stream().map(this::data).toList();
    }
    @PostMapping public ApplicationData add(@AuthenticationPrincipal String email, @RequestBody ApplicationInput input) {
        var user = users.findByEmail(email).orElseThrow();
        var item = items.findById(input.itemId()).filter(found -> found.isActive() && "job".equals(found.getKind()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vaga não encontrada."));
        var application = applications.findByUserIdAndItemId(user.getId(), item.getId()).orElseGet(JobApplication::new);
        application.setUser(user); application.setItem(item);
        return data(applications.save(application));
    }
    @PutMapping("/{id}") public ApplicationData update(@AuthenticationPrincipal String email, @PathVariable Long id, @RequestBody StatusInput input) {
        Long userId = users.findByEmail(email).orElseThrow().getId();
        if (!STATUSES.contains(input.status())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status inválido.");
        var application = applications.findByIdAndUserId(id, userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        application.setStatus(input.status()); application.setUpdatedAt(Instant.now());
        return data(applications.save(application));
    }
    @PutMapping("/{id}/details") public ApplicationData updateDetails(@AuthenticationPrincipal String email, @PathVariable Long id, @RequestBody DetailsInput input) {
        Long userId = users.findByEmail(email).orElseThrow().getId();
        var application = applications.findByIdAndUserId(id, userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        application.setNotes(input.notes() == null || input.notes().isBlank() ? null : input.notes().trim());
        application.setFollowUpDate(input.followUpDate()); application.setReminderSentAt(null); application.setUpdatedAt(Instant.now());
        return data(applications.save(application));
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@AuthenticationPrincipal String email, @PathVariable Long id) {
        Long userId = users.findByEmail(email).orElseThrow().getId();
        applications.delete(applications.findByIdAndUserId(id, userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
    }
    private ApplicationData data(JobApplication application) {
        CatalogItem item = application.getItem();
        return new ApplicationData(application.getId(), item.getId(), item.getTitle(), item.getProvider(), item.getLocation(), item.getUrl(), application.getStatus(), application.getCreatedAt(), application.getUpdatedAt(), application.getNotes(), application.getFollowUpDate());
    }
}
