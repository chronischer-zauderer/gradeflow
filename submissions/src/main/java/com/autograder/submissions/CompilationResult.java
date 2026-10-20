package com.autograder.submissions;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "compilation_results")
public class CompilationResult {

    @Id
    private UUID id;

    @Column(name = "submission_id", nullable = false)
    private UUID submissionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubmissionStatus status;

    @Column(name = "exit_code", nullable = false)
    private int exitCode;

    @Column(columnDefinition = "text")
    private String stdout;

    @Column(columnDefinition = "text")
    private String stderr;

    @Column(name = "duration_ms", nullable = false)
    private long durationMilliseconds;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected CompilationResult() {
    }

    public CompilationResult(UUID submissionId, SubmissionStatus status, int exitCode,
                             String stdout, String stderr, long durationMilliseconds) {
        this.id = UUID.randomUUID();
        this.submissionId = submissionId;
        this.status = status;
        this.exitCode = exitCode;
        this.stdout = stdout;
        this.stderr = stderr;
        this.durationMilliseconds = durationMilliseconds;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getSubmissionId() {
        return submissionId;
    }

    public SubmissionStatus getStatus() {
        return status;
    }

    public int getExitCode() {
        return exitCode;
    }

    public String getStdout() {
        return stdout;
    }

    public String getStderr() {
        return stderr;
    }

    public long getDurationMilliseconds() {
        return durationMilliseconds;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
