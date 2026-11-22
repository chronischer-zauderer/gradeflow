package com.autograder.shared.contracts;

import java.util.List;

/** Immutable grading output produced from an execution artifact. */
public record GradingResult(
        double score,
        double maximumScore,
        boolean approved,
        List<CriterionResult> criteria,
        String summary) {

    public GradingResult {
        criteria = criteria == null ? List.of() : List.copyOf(criteria);
        summary = summary == null ? "" : summary;
    }

    /** Score and explanation for one rubric criterion or test case. */
    public record CriterionResult(
            String criterion,
            double score,
            double maximumScore,
            boolean passed,
            String summary) {
    }
}
