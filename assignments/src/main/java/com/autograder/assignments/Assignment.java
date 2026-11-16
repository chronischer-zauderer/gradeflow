package com.autograder.assignments;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "assignments")
public class Assignment {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "course_id")
    private UUID courseId;

    @Column(nullable = false)
    private String language;

    @Column(name = "due_date")
    private Instant dueDate;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Assignment() {
    }

    public Assignment(UUID id, String name, String language, Instant createdAt) {
        this(id, null, name, "", language, null, createdAt);
    }

    public Assignment(UUID id, UUID courseId, String title, String description, String language,
                      Instant dueDate, Instant createdAt) {
        this.id = id;
        this.courseId = courseId;
        this.title = title;
        this.description = description;
        this.language = language;
        this.dueDate = dueDate;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return title;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public UUID getCourseId() {
        return courseId;
    }

    public String getLanguage() {
        return language;
    }

    public Instant getDueDate() {
        return dueDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
