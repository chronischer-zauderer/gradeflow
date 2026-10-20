package com.autograder.appworker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.autograder.assignments.Assignment;
import com.autograder.assignments.AssignmentRepository;
import com.autograder.sandbox.SandboxExecutor;
import com.autograder.sandbox.SandboxResult;
import com.autograder.shared.Queues;
import com.autograder.submissions.Submission;
import com.autograder.submissions.SubmissionRepository;
import com.autograder.submissions.SubmissionStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Worker side with real Postgres + RabbitMQ: a queued submission flows through the listener and
 * orchestrator to a persisted final status. The sandbox itself is mocked so the test needs no
 * Docker-in-Docker. Schema is created from the entities (the Flyway migration lives in app-api).
 */
@Testcontainers
@SpringBootTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class EvaluationFlowIntegrationTest {

    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container @ServiceConnection
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3-management-alpine");

    @MockitoBean SandboxExecutor sandbox;

    @Autowired AssignmentRepository assignments;
    @Autowired SubmissionRepository submissions;
    @Autowired RabbitTemplate rabbitTemplate;

    @Test
    void validCodeEndsCompiled() {
        when(sandbox.compile(any())).thenReturn(new SandboxResult(0, "", "", 15));
        UUID id = queueSubmission("print('ok')");
        awaitStatus(id, SubmissionStatus.COMPILED);
    }

    @Test
    void invalidCodeEndsCompileError() {
        when(sandbox.compile(any())).thenReturn(new SandboxResult(1, "", "SyntaxError: invalid syntax", 11));
        UUID id = queueSubmission("def broken(:");
        awaitStatus(id, SubmissionStatus.COMPILE_ERROR);
    }

    private UUID queueSubmission(String code) {
        UUID assignmentId = UUID.randomUUID();
        assignments.save(new Assignment(assignmentId, "demo", "PYTHON", Instant.now()));
        Submission submission = submissions.save(new Submission(assignmentId, code));
        rabbitTemplate.convertAndSend(Queues.SUBMISSIONS, submission.getId().toString());
        return submission.getId();
    }

    private void awaitStatus(UUID id, SubmissionStatus expected) {
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(submissions.findById(id)).get()
                        .extracting(Submission::getStatus).isEqualTo(expected));
    }
}
