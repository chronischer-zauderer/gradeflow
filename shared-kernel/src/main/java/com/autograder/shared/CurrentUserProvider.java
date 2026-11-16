package com.autograder.shared;

import java.util.Optional;

/** Port used by domain services to read the current authenticated principal. */
public interface CurrentUserProvider {

    Optional<AuthenticatedUser> currentUser();

    default AuthenticatedUser requireUser() {
        return currentUser().orElseThrow(() -> new BusinessException(401, "Authentication required"));
    }
}
