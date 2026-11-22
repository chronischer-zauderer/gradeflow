package com.autograder.identity;

import com.autograder.shared.AuthenticatedUser;
import com.autograder.shared.BusinessException;
import com.autograder.shared.UserRole;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean({PasswordEncoder.class, JwtTokenService.class})
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokens;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtTokenService tokens) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
    }

    @Transactional
    public AuthResult register(String email, String password, String fullName, UserRole role) {
        if (role == UserRole.ADMIN) {
            throw new BusinessException(400, "ADMIN registration is not allowed");
        }
        String normalizedEmail = normalizeEmail(email);
        if (users.existsByEmail(normalizedEmail)) {
            throw new BusinessException(409, "Email already registered");
        }
        User user = users.save(new User(UUID.randomUUID(), normalizedEmail,
                passwordEncoder.encode(password), fullName.trim(), role, Instant.now()));
        return result(user);
    }

    @Transactional(readOnly = true)
    public AuthResult login(String email, String password) {
        User user = users.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new BusinessException(401, "Invalid credentials"));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException(401, "Invalid credentials");
        }
        return result(user);
    }

    private AuthResult result(User user) {
        return new AuthResult(new UserView(user.getId(), user.getEmail(), user.getFullName(), user.getRole()),
                tokens.generate(user), tokens.getExpirationMs());
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BusinessException(400, "Email must not be empty");
        }
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public record UserView(UUID id, String email, String fullName, UserRole role) {
    }

    public record AuthResult(UserView user, String accessToken, long expiresIn) {
    }
}
