package com.autograder.orchestrator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.autograder.sandbox.SandboxException;
import com.autograder.sandbox.SandboxExecutor;
import com.autograder.sandbox.SandboxResult;
import com.autograder.submissions.CompilationResult;
import com.autograder.submissions.CompilationResultRepository;
import com.autograder.submissions.Submission;
import com.autograder.submissions.SubmissionRepository;
import com.autograder.submissions.SubmissionStatus;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EvaluationServiceTest {

    @Mock SubmissionRepository submissions;
    @Mock CompilationResultRepository results;
    @Mock SandboxExecutor sandbox;
    @Captor ArgumentCaptor<CompilationResult> resultCaptor;

    EvaluationService service;
    Submission submission;

    @BeforeEach
    void setUp() {
        service = new EvaluationService(submissions, results, sandbox);
        submission = new Submission(UUID.randomUUID(), "print('hi')");
        when(submissions.findById(submission.getId())).thenReturn(Optional.of(submission));
    }

    @Test
    void exitZeroBecomesCompiled() {
        when(sandbox.compile(any())).thenReturn(new SandboxResult(0, "", "", 12));
        service.evaluate(submission.getId());
        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.COMPILED);
        assertThat(savedResult().getStatus()).isEqualTo(SubmissionStatus.COMPILED);
    }

    @Test
    void nonZeroBecomesCompileError() {
        when(sandbox.compile(any())).thenReturn(new SandboxResult(1, "", "SyntaxError", 9));
        service.evaluate(submission.getId());
        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.COMPILE_ERROR);
        assertThat(savedResult().getStderr()).isEqualTo("SyntaxError");
    }

    @Test
    void infraFailureBecomesFailed() {
        when(sandbox.compile(any())).thenThrow(new SandboxException("timed out"));
        service.evaluate(submission.getId());
        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.FAILED);
        assertThat(savedResult().getStatus()).isEqualTo(SubmissionStatus.FAILED);
    }

    private CompilationResult savedResult() {
        org.mockito.Mockito.verify(results).save(resultCaptor.capture());
        return resultCaptor.getValue();
    }
}
