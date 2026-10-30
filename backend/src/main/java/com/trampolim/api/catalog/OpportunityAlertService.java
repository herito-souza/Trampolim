package com.trampolim.api.catalog;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trampolim.api.user.AppUser;
import com.trampolim.api.user.AppUserRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OpportunityAlertService {
    private final AppUserRepository users;
    private final CatalogItemRepository items;
    private final ObjectMapper mapper;
    private final ObjectProvider<JavaMailSender> mailSenders;
    @org.springframework.beans.factory.annotation.Value("${app.mail.enabled:false}") private boolean mailEnabled;

    public void published(CatalogItem item) {
        if (!item.isActive() || !"job".equals(item.getKind())) return;
        for (AppUser user : users.findAllByAlertPreferencesIsNotNull()) {
            try {
                var prefs = mapper.readTree(user.getAlertPreferences());
                if (!prefs.path("enabled").asBoolean(false) || !matches(prefs, item)) continue;
                String frequency = prefs.path("frequency").asText("weekly");
                if ("immediate".equals(frequency) && send(user, List.of(item))) continue;
                else {
                    List<Long> pending = readPending(user);
                    if (!pending.contains(item.getId())) pending.add(item.getId());
                    user.setPendingAlertIds(mapper.writeValueAsString(pending));
                    users.save(user);
                }
            } catch (Exception ignored) { /* Preferential data is user-controlled; one malformed record must not block publication. */ }
        }
    }

    @org.springframework.beans.factory.annotation.Value("${operations.catalog-jobs-enabled:true}") private boolean jobsEnabled=true;

    @Scheduled(cron = "0 0 9 * * *", zone = "America/Sao_Paulo")
    public void sendDigests() {
        if (!jobsEnabled) return;
        boolean weeklyDay = java.time.LocalDate.now(java.time.ZoneId.of("America/Sao_Paulo")).getDayOfWeek() == java.time.DayOfWeek.MONDAY;
        for (AppUser user : users.findAllByAlertPreferencesIsNotNull()) {
            try {
                var prefs = mapper.readTree(user.getAlertPreferences());
                String frequency = prefs.path("frequency").asText("weekly");
                if (!prefs.path("enabled").asBoolean(false) || ("weekly".equals(frequency) && !weeklyDay)) continue;
                List<CatalogItem> digest = readPending(user).stream().map(items::findById).flatMap(java.util.Optional::stream)
                        .filter(CatalogItem::isActive).filter(item -> matches(prefs, item)).toList();
                if (!digest.isEmpty() && send(user, digest)) { user.setPendingAlertIds("[]"); users.save(user); }
            } catch (Exception ignored) { /* Continue sending other users' digests. */ }
        }
    }

    private List<Long> readPending(AppUser user) throws Exception {
        return user.getPendingAlertIds() == null ? new ArrayList<>() : mapper.readValue(user.getPendingAlertIds(), new TypeReference<>() {});
    }
    private boolean matches(com.fasterxml.jackson.databind.JsonNode prefs, CatalogItem item) {
        if (!prefs.path("areas").toString().contains("\"" + item.getArea() + "\"")) return false;
        if (prefs.path("useDistance").asBoolean(false) && !withinRadius(prefs, item)) return false;
        return containsOrEmpty(prefs.path("city").asText(""), item.getLocation())
                && equalsOrEmpty(prefs.path("modality").asText(""), item.getModality())
                && equalsOrEmpty(prefs.path("contractType").asText(""), item.getContractType())
                && equalsOrEmpty(prefs.path("workSchedule").asText(""), item.getWorkSchedule())
                && keywordMatch(prefs.path("keywords").asText(""), item.getTitle() + " " + item.getDescription() + " " + item.getProvider());
    }
    private boolean withinRadius(com.fasterxml.jackson.databind.JsonNode prefs, CatalogItem item) {
        if (item.getLatitude() == null || item.getLongitude() == null || !prefs.hasNonNull("latitude")
                || !prefs.hasNonNull("longitude") || !prefs.hasNonNull("radiusKm")) return false;
        double userLat = prefs.path("latitude").asDouble(); double userLon = prefs.path("longitude").asDouble();
        double lat1 = Math.toRadians(userLat); double lat2 = Math.toRadians(item.getLatitude());
        double dLat = Math.toRadians(item.getLatitude() - userLat); double dLon = Math.toRadians(item.getLongitude() - userLon);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        a = Math.max(0, Math.min(1, a));
        double km = 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return km <= prefs.path("radiusKm").asInt();
    }
    private boolean containsOrEmpty(String filter, String value) {
        return filter == null || filter.isBlank() || (value != null && value.toLowerCase(java.util.Locale.ROOT).contains(filter.trim().toLowerCase(java.util.Locale.ROOT)));
    }
    private boolean equalsOrEmpty(String filter, String value) {
        return filter == null || filter.isBlank() || (value != null && filter.equalsIgnoreCase(value));
    }
    private boolean keywordMatch(String filters, String searchableText) {
        if (filters == null || filters.isBlank()) return true;
        String searchable = searchableText == null ? "" : searchableText.toLowerCase(java.util.Locale.ROOT);
        return java.util.Arrays.stream(filters.split(",")).map(String::trim).filter(term -> !term.isEmpty())
                .anyMatch(term -> searchable.contains(term.toLowerCase(java.util.Locale.ROOT)));
    }
    private boolean send(AppUser user, List<CatalogItem> matches) {
        JavaMailSender sender = mailSenders.getIfAvailable();
        if (!mailEnabled || sender == null) return false;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail()); message.setSubject("Novas oportunidades para você no Trampolim");
        message.setText("Encontramos " + matches.size() + " nova(s) oportunidade(s) nas áreas selecionadas:\n\n" + matches.stream().map(item -> "• " + item.getTitle() + " — " + item.getProvider() + (item.getLocation() == null ? "" : " (" + item.getLocation() + ")") + "\n" + item.getUrl()).collect(java.util.stream.Collectors.joining("\n\n")));
        try { sender.send(message); return true; } catch (RuntimeException ignored) { return false; }
    }
}
