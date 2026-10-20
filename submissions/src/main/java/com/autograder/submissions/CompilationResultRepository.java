package com.autograder.submissions;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompilationResultRepository extends JpaRepository<CompilationResult, UUID> {
    Optional<CompilationResult> findBySubmissionId(UUID submissionId);
}
