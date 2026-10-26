package com.trampolim.api.user;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/premium")
@RequiredArgsConstructor
public class PremiumController {
    private final JdbcTemplate jdbc;
    private final RestClient restClient = RestClient.builder()
            .baseUrl("https://api.openai.com")
            .build();

    @Value("${openai.api-key:}") private String apiKey;
    @Value("${openai.model:gpt-5-mini}") private String model;
    private final ConcurrentHashMap<String, RateWindow> feedbackRateLimits = new ConcurrentHashMap<>();
    private record RateWindow(Instant resetAt, int count) {}
    public record PremiumStatus(boolean interviewAiAvailable, boolean professionalSchedulingAvailable, boolean paymentsEnabled) {}
    public record InterviewFeedbackInput(String area, String question, String answer) {}
    public record InterviewFeedback(String feedback, String provider) {}
    public record InterestInput(String type, String details) {}
    public record InterestView(String type, String details, String updatedAt) {}

    @GetMapping("/status")
    public PremiumStatus status() {
        return new PremiumStatus(apiKey != null && !apiKey.isBlank(), false, false);
    }

    @PostMapping("/interview-feedback")
    public InterviewFeedback interviewFeedback(@AuthenticationPrincipal String email, @RequestBody InterviewFeedbackInput input) {
        if (input == null || blank(input.question()) || blank(input.answer()) || input.question().length() > 400 || input.answer().length() > 2500)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe a pergunta e uma resposta de até 2.500 caracteres.");
        limitFeedback(email);
        if (apiKey == null || apiKey.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "O feedback por IA ainda não foi configurado neste servidor.");

        String area = input.area() == null ? "geral" : input.area().trim().substring(0, Math.min(input.area().trim().length(), 80));
        String prompt = "Área: " + area + "\nPergunta: " + input.question().trim() + "\nResposta da pessoa: " + input.answer().trim();
        try {
            JsonNode response = restClient.post().uri("/v1/responses")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "model", model,
                            "instructions", "Você é um orientador de carreira para simulações de entrevista. Responda em português do Brasil, com tom respeitoso e encorajador. Não decida se a pessoa deve ser contratada, não invente fatos sobre a experiência dela, não faça avaliação psicológica ou diagnóstico. Dê feedback breve e concreto: um ponto forte observado, uma melhoria prática, e uma pergunta de reflexão. Se a resposta mencionar discriminação, violência, sofrimento emocional ou dados sensíveis, acolha brevemente e sugira procurar apoio humano apropriado, sem fazer aconselhamento clínico.",
                            "input", prompt,
                            "max_output_tokens", 350))
                    .retrieve().body(JsonNode.class);
            String text = extractOutputText(response);
            if (text.isBlank()) throw new IllegalStateException("Resposta vazia do serviço de IA.");
            return new InterviewFeedback(text, "openai");
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Não foi possível obter feedback da IA agora. Tente novamente mais tarde.");
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "A IA não retornou um feedback utilizável. Tente novamente.");
        }
    }

    @GetMapping("/interests")
    public List<InterestView> interests(@AuthenticationPrincipal String email) {
        return jdbc.query("SELECT i.interest_type, i.details, i.updated_at FROM premium_interests i JOIN app_users u ON u.id=i.user_id WHERE LOWER(u.email)=LOWER(?) ORDER BY i.updated_at DESC",
                (rs, row) -> new InterestView(rs.getString(1), rs.getString(2), rs.getTimestamp(3).toInstant().toString()), email);
    }

    @PostMapping("/interests")
    public InterestView registerInterest(@AuthenticationPrincipal String email, @RequestBody InterestInput input) {
        if (input == null || input.type() == null || !List.of("PILOT", "GUIDANCE").contains(input.type()) || (input.details() != null && input.details().length() > 600))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione uma opção válida para registrar seu interesse.");
        return jdbc.queryForObject("INSERT INTO premium_interests(user_id, interest_type, details) SELECT id, ?, ? FROM app_users WHERE LOWER(email)=LOWER(?) ON CONFLICT (user_id, interest_type) DO UPDATE SET details=EXCLUDED.details, updated_at=CURRENT_TIMESTAMP RETURNING interest_type, details, updated_at",
                (rs, row) -> new InterestView(rs.getString(1), rs.getString(2), rs.getTimestamp(3).toInstant().toString()), input.type(), input.details() == null ? "" : input.details().trim(), email);
    }

    private String extractOutputText(JsonNode response) {
        if (response == null) return "";
        if (response.hasNonNull("output_text")) return response.get("output_text").asText("").trim();
        StringBuilder result = new StringBuilder();
        for (JsonNode item : response.path("output")) for (JsonNode content : item.path("content"))
            if ("output_text".equals(content.path("type").asText())) result.append(content.path("text").asText()).append('\n');
        return result.toString().trim();
    }

    private void limitFeedback(String email) {
        Instant now = Instant.now();
        RateWindow window = feedbackRateLimits.compute(email.toLowerCase(), (key, old) -> old == null || old.resetAt().isBefore(now)
                ? new RateWindow(now.plusSeconds(60), 1) : new RateWindow(old.resetAt(), old.count() + 1));
        if (window.count() > 8) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Você atingiu o limite de simulações por minuto. Aguarde um pouco e tente novamente.");
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
}
