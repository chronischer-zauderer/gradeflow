package com.autograder.appapi;

import com.autograder.identity.AuthService;
import com.autograder.shared.UserRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    public record AuthRequest(@Email @NotBlank String email, @NotBlank String password) { }

    public record RegisterRequest(@Email @NotBlank String email, @NotBlank String password,
                                  @NotBlank String fullName, @NotNull UserRole role) { }

    public record AuthResponse(String accessToken, String tokenType, long expiresIn,
                               AuthService.UserView user) { }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(response(auth.register(
                request.email(), request.password(), request.fullName(), request.role())));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthRequest request) {
        return response(auth.login(request.email(), request.password()));
    }

    private AuthResponse response(AuthService.AuthResult result) {
        return new AuthResponse(result.accessToken(), "Bearer", result.expiresIn(), result.user());
    }
}
