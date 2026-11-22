package com.autograder.assignments;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.autograder.shared.AuthenticatedUser;
import com.autograder.shared.BusinessException;
import com.autograder.shared.UserRole;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock CourseRepository courses;
    @Mock CourseEnrollmentRepository enrollments;
    @Mock AssignmentRepository assignments;

    CourseService service;
    UUID courseId;
    UUID teacherId;

    @BeforeEach
    void setUp() {
        service = new CourseService(courses, enrollments, assignments);
        courseId = UUID.randomUUID();
        teacherId = UUID.randomUUID();
    }

    @Test
    void rejectsTeacherCreatingAssignmentInAnotherTeachersCourse() {
        Course course = new Course(courseId, "Course", "", "ABCDEFGH", teacherId, Instant.now());
        when(courses.findById(courseId)).thenReturn(Optional.of(course));
        AuthenticatedUser otherTeacher = new AuthenticatedUser(UUID.randomUUID(), "other@example.com", UserRole.TEACHER);

        assertThatThrownBy(() -> service.createAssignment(otherTeacher, courseId, "Task", "", "PYTHON", null))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> org.assertj.core.api.Assertions.assertThat(((BusinessException) error).getStatus()).isEqualTo(403));
    }

    @Test
    void rejectsStudentJoiningUnknownInviteCode() {
        when(courses.findByInviteCode("UNKNOWN")).thenReturn(Optional.empty());
        AuthenticatedUser student = new AuthenticatedUser(UUID.randomUUID(), "student@example.com", UserRole.STUDENT);

        assertThatThrownBy(() -> service.join(student, "unknown"))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> org.assertj.core.api.Assertions.assertThat(((BusinessException) error).getStatus()).isEqualTo(404));
    }
}
