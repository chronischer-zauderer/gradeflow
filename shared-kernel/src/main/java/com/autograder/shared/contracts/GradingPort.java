package com.autograder.shared.contracts;

/** Extension point for deterministic test-case or rubric-based grading. */
public interface GradingPort {

    GradingResult evaluate(ExecutionArtifact artifact);
}
