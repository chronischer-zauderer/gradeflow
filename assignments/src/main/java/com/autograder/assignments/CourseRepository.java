package com.autograder.assignments;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, UUID> {
    Optional<Course> findByInviteCode(String inviteCode);
    List<Course> findByTeacherId(UUID teacherId);
    boolean existsByInviteCode(String inviteCode);
}
