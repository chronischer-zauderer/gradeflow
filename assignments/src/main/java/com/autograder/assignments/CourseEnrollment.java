package com.autograder.assignments;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "course_enrollments")
public class CourseEnrollment {
    @Id private UUID id;
    @Column(name = "course_id", nullable = false) private UUID courseId;
    @Column(name = "student_id", nullable = false) private UUID studentId;
    @Column(name = "enrolled_at", nullable = false) private Instant enrolledAt;

    protected CourseEnrollment() { }

    public CourseEnrollment(UUID id, UUID courseId, UUID studentId, Instant enrolledAt) {
        this.id = id;
        this.courseId = courseId;
        this.studentId = studentId;
        this.enrolledAt = enrolledAt;
    }

    public UUID getId() { return id; }
    public UUID getCourseId() { return courseId; }
    public UUID getStudentId() { return studentId; }
    public Instant getEnrolledAt() { return enrolledAt; }
}
