package com.trampolim.api.user;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/premium")
@RequiredArgsConstructor
public class PremiumController {
    private final JdbcTemplate jdbc;
    @org.springframework.beans.factory.annotation.Autowired(required=false) private com.trampolim.api.career.CareerAiService careerAi;
    public record PremiumStatus(boolean interviewAiAvailable, boolean professionalSchedulingAvailable, boolean paymentsEnabled) {}
    public record InterviewFeedbackInput(String area, String question, String answer, java.util.UUID clientKey, boolean consent) { public InterviewFeedbackInput(String area,String question,String answer){this(area,question,answer,null,false);} }
    public record InterviewFeedback(String feedback, String provider) {}
    public record InterestInput(String type, String details) {}
    public record InterestView(String type, String details, String updatedAt) {}

    @GetMapping("/status")
    public PremiumStatus status() {
        return new PremiumStatus(careerAi != null && careerAi.available(), false, false);
    }

    @PostMapping("/interview-feedback")
    public InterviewFeedback interviewFeedback(@AuthenticationPrincipal String email, @RequestBody InterviewFeedbackInput input) {
        if (input == null || blank(input.question()) || blank(input.answer()) || input.question().length() > 400 || input.answer().length() > 2500)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe a pergunta e uma resposta de até 2.500 caracteres.");
        if(careerAi==null)throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"O feedback por IA ainda não foi configurado neste servidor.");
        var result=careerAi.ask(email,new com.trampolim.api.career.CareerAiService.Ask(input.clientKey(),"INTERVIEW","Área: "+input.area()+"; pergunta: "+input.question()+"; resposta: "+input.answer(),true,input.consent()));
        String feedback=java.util.stream.Stream.of("strengths","improvements","nextSteps").map(field->String.join("; ",(List<String>)result.getOrDefault(field,List.of()))).collect(java.util.stream.Collectors.joining("\n"));
        return new InterviewFeedback(feedback,"openai");
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

    private boolean blank(String value) { return value == null || value.isBlank(); }
}
