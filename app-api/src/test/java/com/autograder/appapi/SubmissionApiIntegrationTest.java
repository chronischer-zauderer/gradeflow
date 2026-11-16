package com.autograder.appapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Authenticated course, enrollment, assignment and submission flow. */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SubmissionApiIntegrationTest {

    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container @ServiceConnection
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3-management-alpine");

    @Autowired TestRestTemplate rest;

    @Test
    void authenticatedStudentSubmitsAndReadsOwnSubmission() {
        String teacherToken = register("teacher@example.com", "Teacher One", "TEACHER");
        ResponseEntity<Map> course = rest.exchange("/api/v1/courses", HttpMethod.POST,
                entity(teacherToken, Map.of("name", "Algorithms", "description", "Core")), Map.class);
        assertThat(course.getStatusCode()).isEqualTo(HttpStatus.OK);
        String inviteCode = (String) course.getBody().get("inviteCode");
        String courseId = (String) course.getBody().get("id");

        String studentToken = register("student@example.com", "Student One", "STUDENT");
        ResponseEntity<Map> joined = rest.exchange("/api/v1/courses/join", HttpMethod.POST,
                entity(studentToken, Map.of("inviteCode", inviteCode)), Map.class);
        assertThat(joined.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Map> assignment = rest.exchange("/api/v1/courses/" + courseId + "/assignments",
                HttpMethod.POST, entity(teacherToken, Map.of("title", "Exercise 1", "language", "PYTHON")), Map.class);
        assertThat(assignment.getStatusCode()).isEqualTo(HttpStatus.OK);
        String assignmentId = (String) assignment.getBody().get("id");

        ResponseEntity<Map> post = rest.exchange("/api/v1/assignments/" + assignmentId + "/submissions",
                HttpMethod.POST, entity(studentToken, Map.of("sourceCode", "print('hi')")), Map.class);
        assertThat(post.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        String submissionId = (String) post.getBody().get("submissionId");

        ResponseEntity<Map> get = rest.exchange("/api/v1/submissions/" + submissionId,
                HttpMethod.GET, entity(studentToken, null), Map.class);
        assertThat(get.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(get.getBody().get("status")).isEqualTo("QUEUED");
    }

    @Test
    void unauthenticatedSubmissionIsRejected() {
        ResponseEntity<Map> response = rest.postForEntity(
                "/api/v1/assignments/11111111-1111-1111-1111-111111111111/submissions",
                Map.of("sourceCode", "print('hi')"), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().get("error")).isEqualTo("Authentication required");
    }

    private String register(String email, String fullName, String role) {
        ResponseEntity<Map> response = rest.postForEntity("/api/v1/auth/register",
                Map.of("email", email, "password", "Password123!", "fullName", fullName, "role", role), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return (String) response.getBody().get("accessToken");
    }

    private HttpEntity<Map<String, Object>> entity(String token, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}
