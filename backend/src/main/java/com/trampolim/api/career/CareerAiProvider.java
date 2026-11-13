package com.trampolim.api.career;

/** Provider boundary for optional career guidance; implementations must not persist input. */
public interface CareerAiProvider {
    String id();
    boolean configured();
    String generate(String instructions, String input, int maxOutputTokens);
}
