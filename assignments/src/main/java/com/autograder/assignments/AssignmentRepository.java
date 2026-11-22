package com.autograder.assignments;

import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {
	List<Assignment> findByCourseId(UUID courseId);
}
