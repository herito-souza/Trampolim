package com.trampolim.api.catalog;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JobReminderService {
    private final JobApplicationRepository applications;
    private final ObjectProvider<JavaMailSender> mailSenders;
    @Value("${app.mail.enabled:false}") private boolean mailEnabled;
    @Value("${app.frontend-url:http://localhost:5173}") private String frontendUrl;

    @org.springframework.beans.factory.annotation.Value("${operations.catalog-jobs-enabled:true}") private boolean jobsEnabled=true;

    @Scheduled(cron = "0 0 9 * * *", zone = "America/Sao_Paulo")
    public void sendFollowUpReminders() {
        if (!jobsEnabled) return;
        if (!mailEnabled) return;
        JavaMailSender sender = mailSenders.getIfAvailable();
        if (sender == null) return;
        LocalDate today = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        applications.findAllByReminderSentAtIsNullAndFollowUpDateIsNotNull().stream()
                .filter(application -> !application.getFollowUpDate().isAfter(today) && !application.getFollowUpDate().isBefore(today.minusDays(2)))
                .filter(application -> !"REJECTED".equals(application.getStatus()) && !"OFFER".equals(application.getStatus()))
                .forEach(application -> {
                    try {
                        var user = application.getUser(); var item = application.getItem();
                        SimpleMailMessage message = new SimpleMailMessage(); message.setTo(user.getEmail());
                        message.setSubject("Lembrete de acompanhamento · " + item.getTitle());
                        message.setText("Você marcou hoje para acompanhar a vaga “" + item.getTitle() + "” na empresa " + item.getProvider() + ".\n\nAcesse seu perfil no Trampolim para atualizar o andamento: " + frontendUrl);
                        sender.send(message); application.setReminderSentAt(Instant.now()); applications.save(application);
                    } catch (RuntimeException ignored) { /* Keep the reminder pending so it can be retried later. */ }
                });
    }
}
