package com.trampolim.api.auth;

import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Shared validation for new credentials; never truncates or normalizes passwords. */
final class PasswordPolicy {
    private PasswordPolicy() {}
    static void validate(String password) {
        if (password == null || password.length() < 8)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A senha deve ter pelo menos 8 caracteres.");
        if (password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A senha excede o limite de 72 bytes. Use uma senha mais curta.");
    }
}
