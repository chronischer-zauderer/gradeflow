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
import com.autograder.shared.contracts.AiEvaluationPort;
import com.autograder.shared.contracts.AiFeedbackResult;
import com.autograder.shared.contracts.GradingPort;
import com.autograder.shared.contracts.GradingResult;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EvaluationServiceTest {

    @Mock SubmissionRepository submissions;
    @Mock CompilationResultRepository results;
    @Mock SandboxExecutor sandbox;
    @Mock GradingPort grading;
    @Mock AiEvaluationPort aiEvaluation;
    @Captor ArgumentCaptor<CompilationResult> resultCaptor;

    EvaluationService service;
    Submission submission;

    @BeforeEach
    void setUp() {
        service = new EvaluationService(submissions, results, sandbox, grading, aiEvaluation);
        submission = new Submission(UUID.randomUUID(), "print('hi')");
        when(submissions.findById(submission.getId())).thenReturn(Optional.of(submission));
    }

    @Test
    void exitZeroBecomesCompiled() {
        when(sandbox.compile(any())).thenReturn(new SandboxResult(0, "", "", 12));
        when(grading.evaluate(any())).thenReturn(new GradingResult(1, 1, true, java.util.List.of(), "ok"));
        when(aiEvaluation.generateFeedback(any(), any()))
            .thenReturn(new AiFeedbackResult("ok", java.util.List.of(), 1, "test"));
        service.evaluate(submission.getId());
        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.COMPILED);
        assertThat(savedResult().getStatus()).isEqualTo(SubmissionStatus.COMPILED);
    }

    @Test
    void nonZeroBecomesCompileError() {
        when(sandbox.compile(any())).thenReturn(new SandboxResult(1, "", "SyntaxError", 9));
        when(grading.evaluate(any())).thenReturn(new GradingResult(0, 1, false, java.util.List.of(), "error"));
        when(aiEvaluation.generateFeedback(any(), any()))
            .thenReturn(new AiFeedbackResult("error", java.util.List.of(), 1, "test"));
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

    @Test
    void evaluatesArtifactThenGeneratesFeedbackInOrder() {
        when(sandbox.compile(any())).thenReturn(new SandboxResult(0, "out", "", 12));
        GradingResult gradingResult = new GradingResult(1, 1, true, java.util.List.of(), "ok");
        when(grading.evaluate(any())).thenReturn(gradingResult);
        when(aiEvaluation.generateFeedback(any(), any()))
                .thenReturn(new AiFeedbackResult("ok", java.util.List.of(), 1, "test"));

        service.evaluate(submission.getId());

        org.mockito.InOrder order = Mockito.inOrder(sandbox, grading, aiEvaluation);
        order.verify(sandbox).compile(submission.getSourceCode());
        order.verify(grading).evaluate(any());
        order.verify(aiEvaluation).generateFeedback(any(), org.mockito.ArgumentMatchers.same(gradingResult));
    }

    private CompilationResult savedResult() {
        org.mockito.Mockito.verify(results).save(resultCaptor.capture());
        return resultCaptor.getValue();
    }
}
