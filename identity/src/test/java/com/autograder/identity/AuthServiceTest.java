package com.autograder.identity;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.autograder.shared.BusinessException;
import com.autograder.shared.UserRole;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository users;
    @Mock PasswordEncoder encoder;
    @Mock JwtTokenService tokens;

    AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(users, encoder, tokens);
    }

    @Test
    void rejectsAdminSelfRegistration() {
        assertThatThrownBy(() -> service.register("admin@example.com", "secret", "Admin", UserRole.ADMIN))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> org.assertj.core.api.Assertions.assertThat(((BusinessException) error).getStatus()).isEqualTo(400));
    }

    @Test
    void rejectsDuplicateEmail() {
        when(users.existsByEmail("teacher@example.com")).thenReturn(true);
        assertThatThrownBy(() -> service.register("teacher@example.com", "secret", "Teacher", UserRole.TEACHER))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> org.assertj.core.api.Assertions.assertThat(((BusinessException) error).getStatus()).isEqualTo(409));
    }

    @Test
    void rejectsInvalidPassword() {
        User user = new User(UUID.randomUUID(), "student@example.com", "hash", "Student", UserRole.STUDENT, Instant.now());
        when(users.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(encoder.matches("bad", "hash")).thenReturn(false);
        assertThatThrownBy(() -> service.login("student@example.com", "bad"))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> org.assertj.core.api.Assertions.assertThat(((BusinessException) error).getStatus()).isEqualTo(401));
    }
}
