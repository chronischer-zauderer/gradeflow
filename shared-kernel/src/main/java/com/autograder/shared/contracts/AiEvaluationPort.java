package com.autograder.shared.contracts;

/** Extension point for pedagogical feedback generated from execution and grading. */
public interface AiEvaluationPort {

    AiFeedbackResult generateFeedback(ExecutionArtifact artifact, GradingResult gradingResult);
}
