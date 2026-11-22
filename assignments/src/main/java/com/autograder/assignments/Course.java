package com.autograder.assignments;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "courses")
public class Course {
    @Id private UUID id;
    @Column(nullable = false) private String name;
    @Column(columnDefinition = "text") private String description;
    @Column(name = "invite_code", nullable = false, unique = true, length = 8) private String inviteCode;
    @Column(name = "teacher_id", nullable = false) private UUID teacherId;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected Course() { }

    public Course(UUID id, String name, String description, String inviteCode, UUID teacherId, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.inviteCode = inviteCode;
        this.teacherId = teacherId;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getInviteCode() { return inviteCode; }
    public UUID getTeacherId() { return teacherId; }
    public Instant getCreatedAt() { return createdAt; }
}
