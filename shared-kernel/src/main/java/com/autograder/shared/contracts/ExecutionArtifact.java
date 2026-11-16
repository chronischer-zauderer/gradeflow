package com.autograder.shared.contracts;

import java.util.UUID;

/** Immutable execution output passed from the sandbox boundary to downstream evaluators. */
public record ExecutionArtifact(
        UUID submissionId,
        UUID assignmentId,
        String sourceCode,
        String stdout,
        String stderr,
        int exitCode,
        long executionTimeMs,
        boolean timedOut,
        boolean compiled) {
}
