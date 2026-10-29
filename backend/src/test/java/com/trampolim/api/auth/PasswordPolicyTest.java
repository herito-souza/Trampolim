package com.trampolim.api.auth;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
class PasswordPolicyTest {
    @Test void validatesUtf8BytesWithoutTruncation() {
        assertDoesNotThrow(() -> PasswordPolicy.validate("a".repeat(72)));
        assertDoesNotThrow(() -> PasswordPolicy.validate("é".repeat(36)));
        assertThrows(ResponseStatusException.class, () -> PasswordPolicy.validate("a".repeat(73)));
        assertThrows(ResponseStatusException.class, () -> PasswordPolicy.validate("é".repeat(37)));
        assertThrows(ResponseStatusException.class, () -> PasswordPolicy.validate("short"));
        assertThrows(ResponseStatusException.class, () -> PasswordPolicy.validate(null));
    }
}
