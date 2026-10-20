package com.autograder.submissions;

import com.autograder.assignments.Assignment;
import com.autograder.assignments.AssignmentRepository;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubmissionService {

    /** Source larger than this is rejected to keep a single submission from filling the DB. */
    static final int MAX_SOURCE_BYTES = 1_000_000;

    private final SubmissionRepository submissions;
    private final CompilationResultRepository results;
    private final AssignmentRepository assignments;

    public SubmissionService(SubmissionRepository submissions, CompilationResultRepository results,
                             AssignmentRepository assignments) {
        this.submissions = submissions;
        this.results = results;
        this.assignments = assignments;
    }

    @Transactional
    public Submission register(UUID assignmentId, String sourceCode) {
        Assignment assignment = assignments.findById(assignmentId)
                .orElseThrow(() -> SubmissionException.notFound("Assignment not found"));
        if (!"PYTHON".equals(assignment.getLanguage())) {
            throw SubmissionException.validation("Unsupported language: " + assignment.getLanguage());
        }
        if (sourceCode == null || sourceCode.isBlank()) {
            throw SubmissionException.validation("Source code must not be empty");
        }
        if (sourceCode.getBytes(StandardCharsets.UTF_8).length > MAX_SOURCE_BYTES) {
            throw SubmissionException.validation("Source code exceeds the maximum allowed size");
        }
        return submissions.save(new Submission(assignmentId, sourceCode));
    }

    @Transactional(readOnly = true)
    public Submission get(UUID submissionId) {
        return submissions.findById(submissionId)
                .orElseThrow(() -> SubmissionException.notFound("Submission not found"));
    }

    @Transactional(readOnly = true)
    public CompilationResult resultOf(UUID submissionId) {
        return results.findBySubmissionId(submissionId).orElse(null);
    }
}
