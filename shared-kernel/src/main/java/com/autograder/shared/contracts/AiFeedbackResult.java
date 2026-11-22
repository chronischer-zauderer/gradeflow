package com.autograder.shared.contracts;

import java.util.List;

/** Immutable pedagogical feedback produced from execution and grading results. */
public record AiFeedbackResult(
        String feedback,
        List<String> suggestions,
        double confidence,
        String model) {

    public AiFeedbackResult {
        feedback = feedback == null ? "" : feedback;
        suggestions = suggestions == null ? List.of() : List.copyOf(suggestions);
        model = model == null ? "" : model;
    }
}
