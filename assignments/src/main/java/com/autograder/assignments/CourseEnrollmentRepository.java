package com.autograder.assignments;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, UUID> {
    boolean existsByCourseIdAndStudentId(UUID courseId, UUID studentId);
    List<CourseEnrollment> findByStudentId(UUID studentId);
}
