package com.autograder.appapi;

import com.autograder.shared.Queues;
import com.autograder.submissions.CompilationResult;
import com.autograder.submissions.Submission;
import com.autograder.submissions.SubmissionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class SubmissionController {

    private final SubmissionService submissions;
    private final RabbitTemplate rabbit;

    public SubmissionController(SubmissionService submissions, RabbitTemplate rabbit) {
        this.submissions = submissions;
        this.rabbit = rabbit;
    }

    public record SubmissionRequest(@NotBlank String sourceCode) {
    }

    public record SubmissionAccepted(UUID submissionId, String status) {
    }

    public record ResultView(int exitCode, String stdout, String stderr, long durationMilliseconds) {
    }

    public record SubmissionView(UUID submissionId, String status, ResultView result) {
    }

    @PostMapping("/assignments/{assignmentId}/submissions")
    public ResponseEntity<SubmissionAccepted> register(@PathVariable UUID assignmentId,
                                                       @Valid @RequestBody SubmissionRequest request) {
        Submission submission = submissions.register(assignmentId, request.sourceCode());
        rabbit.convertAndSend(Queues.SUBMISSIONS, submission.getId().toString());
        return ResponseEntity.accepted()
                .body(new SubmissionAccepted(submission.getId(), submission.getStatus().name()));
    }

    @GetMapping("/submissions/{submissionId}")
    public SubmissionView get(@PathVariable UUID submissionId) {
        Submission submission = submissions.get(submissionId);
        CompilationResult result = submissions.resultOf(submissionId);
        ResultView view = result == null ? null
                : new ResultView(result.getExitCode(), result.getStdout(), result.getStderr(),
                        result.getDurationMilliseconds());
        return new SubmissionView(submission.getId(), submission.getStatus().name(), view);
    }
}
