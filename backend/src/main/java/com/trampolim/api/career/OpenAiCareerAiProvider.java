package com.trampolim.api.career;

import com.trampolim.api.config.BoundedRestClients;
import com.trampolim.api.radar.RadarBridge;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component("openAiCareerAiProvider")
public class OpenAiCareerAiProvider implements CareerAiProvider {
    private final RadarBridge json;
    @Value("${openai.api-key:}") private String key;
    @Value("${openai.model:gpt-5-mini}") private String model;
    @Value("${mentor.base-url:https://api.openai.com}") private String base;
    @Value("${mentor.allow-test-http:false}") private boolean testHttp;

    public OpenAiCareerAiProvider(RadarBridge json) { this.json = json; }

    @Override public String id() { return "openai"; }
    @Override public boolean configured() { return key != null && !key.isBlank(); }

    @Override public String generate(String instructions, String input, int maxOutputTokens) {
        if (maxOutputTokens < 100 || maxOutputTokens > 2000 ||
                (!base.startsWith("https://") && !testHttp))
            throw new IllegalStateException("Configuração insegura do provedor.");
        return BoundedRestClients.create(base, Duration.ofSeconds(25)).post().uri("/v1/responses")
                .header("Authorization", "Bearer " + key)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("model", model, "instructions", instructions, "input", input,
                        "store", false, "max_output_tokens", maxOutputTokens))
                .retrieve().body(String.class);
    }
}
