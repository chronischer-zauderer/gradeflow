package com.autograder.assignments;

import com.autograder.shared.AuthenticatedUser;
import com.autograder.shared.BusinessException;
import com.autograder.shared.UserRole;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseService {

    private static final char[] CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private final SecureRandom random = new SecureRandom();
    private final CourseRepository courses;
    private final CourseEnrollmentRepository enrollments;
    private final AssignmentRepository assignments;

    public CourseService(CourseRepository courses, CourseEnrollmentRepository enrollments,
                         AssignmentRepository assignments) {
        this.courses = courses;
        this.enrollments = enrollments;
        this.assignments = assignments;
    }

    @Transactional
    public Course createCourse(AuthenticatedUser user, String name, String description) {
        requireRole(user, UserRole.TEACHER, UserRole.ADMIN);
        if (name == null || name.isBlank()) {
            throw new BusinessException(400, "Course name must not be empty");
        }
        String code;
        do {
            code = inviteCode();
        } while (courses.existsByInviteCode(code));
        return courses.save(new Course(UUID.randomUUID(), name.trim(), description == null ? "" : description,
                code, user.userId(), Instant.now()));
    }

    @Transactional(readOnly = true)
    public List<Course> listCourses(AuthenticatedUser user) {
        if (user.role() == UserRole.ADMIN) {
            return courses.findAll();
        }
        if (user.role() == UserRole.TEACHER) {
            return courses.findByTeacherId(user.userId());
        }
        List<Course> result = new ArrayList<>();
        for (CourseEnrollment enrollment : enrollments.findByStudentId(user.userId())) {
            courses.findById(enrollment.getCourseId()).ifPresent(result::add);
        }
        return result;
    }

    @Transactional
    public CourseEnrollment join(AuthenticatedUser user, String inviteCode) {
        requireRole(user, UserRole.STUDENT);
        Course course = courses.findByInviteCode(inviteCode == null ? "" : inviteCode.trim().toUpperCase())
                .orElseThrow(() -> new BusinessException(404, "Course not found"));
        if (enrollments.existsByCourseIdAndStudentId(course.getId(), user.userId())) {
            throw new BusinessException(409, "Student is already enrolled");
        }
        return enrollments.save(new CourseEnrollment(UUID.randomUUID(), course.getId(), user.userId(), Instant.now()));
    }

    @Transactional
    public Assignment createAssignment(AuthenticatedUser user, UUID courseId, String title,
                                       String description, String language, java.time.Instant dueDate) {
        Course course = course(courseId);
        if (user.role() != UserRole.ADMIN &&
                (user.role() != UserRole.TEACHER || !course.getTeacherId().equals(user.userId()))) {
            throw new BusinessException(403, "Only the course teacher or an admin may create assignments");
        }
        if (title == null || title.isBlank()) {
            throw new BusinessException(400, "Assignment title must not be empty");
        }
        String selectedLanguage = language == null || language.isBlank() ? "PYTHON" : language.toUpperCase();
        if (!"PYTHON".equals(selectedLanguage)) {
            throw new BusinessException(400, "Unsupported language: " + selectedLanguage);
        }
        return assignments.save(new Assignment(UUID.randomUUID(), courseId, title.trim(),
                description == null ? "" : description, selectedLanguage, dueDate, Instant.now()));
    }

    @Transactional(readOnly = true)
    public List<Assignment> listAssignments(AuthenticatedUser user, UUID courseId) {
        Course course = course(courseId);
        if (!canAccessCourse(user, course)) {
            throw new BusinessException(403, "You do not have access to this course");
        }
        return assignments.findByCourseId(courseId);
    }

    @Transactional(readOnly = true)
    public Course course(UUID courseId) {
        return courses.findById(courseId).orElseThrow(() -> new BusinessException(404, "Course not found"));
    }

    @Transactional(readOnly = true)
    public boolean enrolled(UUID courseId, UUID studentId) {
        return enrollments.existsByCourseIdAndStudentId(courseId, studentId);
    }

    private boolean canAccessCourse(AuthenticatedUser user, Course course) {
        return user.role() == UserRole.ADMIN
                || (user.role() == UserRole.TEACHER && course.getTeacherId().equals(user.userId()))
                || (user.role() == UserRole.STUDENT && enrolled(course.getId(), user.userId()));
    }

    private String inviteCode() {
        StringBuilder code = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            code.append(CODE_CHARS[random.nextInt(CODE_CHARS.length)]);
        }
        return code.toString();
    }

    private static void requireRole(AuthenticatedUser user, UserRole... roles) {
        for (UserRole role : roles) {
            if (user.role() == role) return;
        }
        throw new BusinessException(403, "Insufficient permissions");
    }
}
