package com.autograder.appapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** API side with real Postgres + RabbitMQ: register a submission and read its status. */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SubmissionApiIntegrationTest {

    // Seeded by Flyway V1 migration.
    private static final String ASSIGNMENT_ID = "11111111-1111-1111-1111-111111111111";

    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container @ServiceConnection
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3-management-alpine");

    @Autowired TestRestTemplate rest;

    @Test
    void registerThenQueryReturnsQueued() {
        ResponseEntity<Map> post = rest.postForEntity(
                "/api/v1/assignments/" + ASSIGNMENT_ID + "/submissions",
                Map.of("sourceCode", "print('hi')"), Map.class);

        assertThat(post.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(post.getBody().get("status")).isEqualTo("QUEUED");
        String id = (String) post.getBody().get("submissionId");

        ResponseEntity<Map> get = rest.getForEntity("/api/v1/submissions/" + id, Map.class);
        assertThat(get.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(get.getBody().get("status")).isEqualTo("QUEUED");
    }

    @Test
    void emptyCodeIsRejected() {
        ResponseEntity<Map> post = rest.postForEntity(
                "/api/v1/assignments/" + ASSIGNMENT_ID + "/submissions",
                Map.of("sourceCode", "   "), Map.class);
        assertThat(post.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
