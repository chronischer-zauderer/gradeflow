package com.autograder.appapi;

import com.autograder.assignments.Assignment;
import com.autograder.assignments.Course;
import com.autograder.assignments.CourseEnrollment;
import com.autograder.assignments.CourseService;
import com.autograder.shared.CurrentUserProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CourseController {

    private final CourseService courses;
    private final CurrentUserProvider currentUser;

    public CourseController(CourseService courses, CurrentUserProvider currentUser) {
        this.courses = courses;
        this.currentUser = currentUser;
    }

    public record CourseRequest(@NotBlank String name, String description) { }
    public record JoinRequest(@NotBlank String inviteCode) { }
    public record AssignmentRequest(@NotBlank String title, String description, String language, Instant dueDate) { }
    public record CourseView(UUID id, String name, String description, String inviteCode, UUID teacherId, Instant createdAt) {
        static CourseView from(Course course) {
            return new CourseView(course.getId(), course.getName(), course.getDescription(),
                    course.getInviteCode(), course.getTeacherId(), course.getCreatedAt());
        }
    }
    public record AssignmentView(UUID id, UUID courseId, String title, String description, String language,
                                 Instant dueDate, Instant createdAt) {
        static AssignmentView from(Assignment assignment) {
            return new AssignmentView(assignment.getId(), assignment.getCourseId(), assignment.getTitle(),
                    assignment.getDescription(), assignment.getLanguage(), assignment.getDueDate(), assignment.getCreatedAt());
        }
    }
    public record EnrollmentView(UUID id, UUID courseId, UUID studentId, Instant enrolledAt) {
        static EnrollmentView from(CourseEnrollment enrollment) {
            return new EnrollmentView(enrollment.getId(), enrollment.getCourseId(), enrollment.getStudentId(), enrollment.getEnrolledAt());
        }
    }

    @PostMapping("/courses")
    public CourseView createCourse(@Valid @RequestBody CourseRequest request) {
        return CourseView.from(courses.createCourse(currentUser.requireUser(), request.name(), request.description()));
    }

    @GetMapping("/courses")
    public List<CourseView> listCourses() {
        return courses.listCourses(currentUser.requireUser()).stream().map(CourseView::from).toList();
    }

    @PostMapping("/courses/join")
    public EnrollmentView join(@Valid @RequestBody JoinRequest request) {
        return EnrollmentView.from(courses.join(currentUser.requireUser(), request.inviteCode()));
    }

    @PostMapping("/courses/{courseId}/assignments")
    public AssignmentView createAssignment(@PathVariable UUID courseId,
                                            @Valid @RequestBody AssignmentRequest request) {
        return AssignmentView.from(courses.createAssignment(currentUser.requireUser(), courseId,
                request.title(), request.description(), request.language(), request.dueDate()));
    }

    @GetMapping("/courses/{courseId}/assignments")
    public List<AssignmentView> listAssignments(@PathVariable UUID courseId) {
        return courses.listAssignments(currentUser.requireUser(), courseId).stream().map(AssignmentView::from).toList();
    }
}
