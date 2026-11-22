package com.autograder.shared;

import java.util.UUID;

/** Authentication context exposed to domain modules without Spring Security coupling. */
public record AuthenticatedUser(UUID userId, String email, UserRole role) {
}
