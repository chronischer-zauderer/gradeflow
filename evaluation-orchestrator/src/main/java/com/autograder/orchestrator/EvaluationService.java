package com.autograder.orchestrator;

import com.autograder.sandbox.SandboxException;
import com.autograder.sandbox.SandboxExecutor;
import com.autograder.sandbox.SandboxResult;
import com.autograder.shared.contracts.AiEvaluationPort;
import com.autograder.shared.contracts.ExecutionArtifact;
import com.autograder.shared.contracts.GradingPort;
import com.autograder.submissions.CompilationResult;
import com.autograder.submissions.CompilationResultRepository;
import com.autograder.submissions.Submission;
import com.autograder.submissions.SubmissionRepository;
import com.autograder.submissions.SubmissionStatus;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Worker-side flow: load a queued submission, run the sandbox, persist the outcome. */
@Service
public class EvaluationService {

    private static final Logger log = LoggerFactory.getLogger(EvaluationService.class);

    private final SubmissionRepository submissions;
    private final CompilationResultRepository results;
    private final SandboxExecutor sandbox;
    private final GradingPort grading;
    private final AiEvaluationPort aiEvaluation;

    public EvaluationService(SubmissionRepository submissions, CompilationResultRepository results,
                             SandboxExecutor sandbox, GradingPort grading,
                             AiEvaluationPort aiEvaluation) {
        this.submissions = submissions;
        this.results = results;
        this.sandbox = sandbox;
        this.grading = grading;
        this.aiEvaluation = aiEvaluation;
    }

    @Transactional
    public void evaluate(UUID submissionId) {
        Submission submission = submissions.findById(submissionId).orElse(null);
        if (submission == null) {
            log.warn("Evaluation skipped: submission {} not found", submissionId);
            return;
        }
        submission.changeStatus(SubmissionStatus.RUNNING);
        submissions.save(submission);

        try {
            SandboxResult r = sandbox.compile(submission.getSourceCode());
            ExecutionArtifact artifact = new ExecutionArtifact(
                    submission.getId(), submission.getAssignmentId(), submission.getSourceCode(),
                    r.stdout(), r.stderr(), r.exitCode(), r.durationMilliseconds(), false,
                    r.compiled());
            try {
                var gradingResult = grading.evaluate(artifact);
                aiEvaluation.generateFeedback(artifact, gradingResult);
            } catch (RuntimeException e) {
                // Optional evaluators must not turn a completed sandbox execution into FAILED.
                log.warn("Optional evaluation unavailable for submission {}: {}", submissionId,
                        e.getMessage());
            }
            SubmissionStatus status = r.compiled() ? SubmissionStatus.COMPILED : SubmissionStatus.COMPILE_ERROR;
            persist(submission, status, r.exitCode(), r.stdout(), r.stderr(), r.durationMilliseconds());
        } catch (SandboxException e) {
            // Infrastructure failure (container/timeout) — controlled FAILED state, never a compile result.
            log.error("Sandbox failure for submission {}: {}", submissionId, e.getMessage());
            persist(submission, SubmissionStatus.FAILED, -1, "", "", 0);
        }
    }

    private void persist(Submission submission, SubmissionStatus status, int exitCode,
                         String stdout, String stderr, long durationMs) {
        results.save(new CompilationResult(submission.getId(), status, exitCode, stdout, stderr, durationMs));
        submission.changeStatus(status);
        submissions.save(submission);
    }
}
