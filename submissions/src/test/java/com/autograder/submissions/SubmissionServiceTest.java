package com.autograder.submissions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.autograder.assignments.Assignment;
import com.autograder.assignments.AssignmentRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

    @Mock SubmissionRepository submissions;
    @Mock CompilationResultRepository results;
    @Mock AssignmentRepository assignments;

    SubmissionService service;
    final UUID assignmentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new SubmissionService(submissions, results, assignments);
    }

    private void assignmentExists(String language) {
        when(assignments.findById(assignmentId))
                .thenReturn(Optional.of(new Assignment(assignmentId, "demo", language, Instant.now())));
    }

    @Test
    void registersValidSubmissionAsQueued() {
        assignmentExists("PYTHON");
        when(submissions.save(any())).thenAnswer(i -> i.getArgument(0));

        Submission result = service.register(assignmentId, "print('hi')");

        assertThat(result.getStatus()).isEqualTo(SubmissionStatus.QUEUED);
        assertThat(result.getAssignmentId()).isEqualTo(assignmentId);
    }

    @Test
    void rejectsEmptyCode() {
        assignmentExists("PYTHON");
        assertThatThrownBy(() -> service.register(assignmentId, "   "))
                .isInstanceOf(SubmissionException.class)
                .satisfies(e -> assertThat(((SubmissionException) e).isNotFound()).isFalse());
    }

    @Test
    void rejectsOversizedCode() {
        assignmentExists("PYTHON");
        String big = "x".repeat(SubmissionService.MAX_SOURCE_BYTES + 1);
        assertThatThrownBy(() -> service.register(assignmentId, big))
                .isInstanceOf(SubmissionException.class);
    }

    @Test
    void rejectsUnsupportedLanguage() {
        assignmentExists("JAVA");
        assertThatThrownBy(() -> service.register(assignmentId, "print('hi')"))
                .isInstanceOf(SubmissionException.class);
    }

    @Test
    void failsWhenAssignmentMissing() {
        when(assignments.findById(assignmentId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.register(assignmentId, "print('hi')"))
                .isInstanceOf(SubmissionException.class)
                .satisfies(e -> assertThat(((SubmissionException) e).isNotFound()).isTrue());
    }
}
