package com.autograder.submissions;

import com.autograder.assignments.Assignment;
import com.autograder.assignments.AssignmentRepository;
import com.autograder.assignments.CourseService;
import com.autograder.assignments.Course;
import com.autograder.shared.AuthenticatedUser;
import com.autograder.shared.BusinessException;
import com.autograder.shared.UserRole;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubmissionService {

    /** Source larger than this is rejected to keep a single submission from filling the DB. */
    static final int MAX_SOURCE_BYTES = 1_000_000;

    private final SubmissionRepository submissions;
    private final CompilationResultRepository results;
    private final AssignmentRepository assignments;
    private final CourseService courses;

    public SubmissionService(SubmissionRepository submissions, CompilationResultRepository results,
                             AssignmentRepository assignments, CourseService courses) {
        this.submissions = submissions;
        this.results = results;
        this.assignments = assignments;
        this.courses = courses;
    }

    @Transactional
    public Submission register(UUID assignmentId, String sourceCode) {
        return registerInternal(assignmentId, sourceCode, null);
    }

    @Transactional
    public Submission register(UUID assignmentId, String sourceCode, AuthenticatedUser user) {
        if (user == null || (user.role() != UserRole.STUDENT && user.role() != UserRole.ADMIN)) {
            throw new BusinessException(403, "Only students or admins may submit code");
        }
        Submission submission = registerInternal(assignmentId, sourceCode, user);
        return submission;
    }

    private Submission registerInternal(UUID assignmentId, String sourceCode, AuthenticatedUser user) {
        Assignment assignment = assignments.findById(assignmentId)
                .orElseThrow(() -> SubmissionException.notFound("Assignment not found"));
        if (!"PYTHON".equals(assignment.getLanguage())) {
            throw SubmissionException.validation("Unsupported language: " + assignment.getLanguage());
        }
        if (sourceCode == null || sourceCode.isBlank()) {
            throw SubmissionException.validation("Source code must not be empty");
        }
        if (sourceCode.getBytes(StandardCharsets.UTF_8).length > MAX_SOURCE_BYTES) {
            throw SubmissionException.validation("Source code exceeds the maximum allowed size");
        }
        if (user != null && user.role() == UserRole.STUDENT) {
            if (assignment.getCourseId() == null || !courses.enrolled(assignment.getCourseId(), user.userId())) {
                throw new BusinessException(403, "Student is not enrolled in this course");
            }
            if (assignment.getDueDate() != null && Instant.now().isAfter(assignment.getDueDate())) {
                throw new BusinessException(422, "Assignment submission deadline has passed");
            }
        }
        return submissions.save(new Submission(assignmentId, user == null ? null : user.userId(), sourceCode));
    }

    @Transactional(readOnly = true)
    public Submission get(UUID submissionId) {
        return submissions.findById(submissionId)
                .orElseThrow(() -> SubmissionException.notFound("Submission not found"));
    }

    @Transactional(readOnly = true)
    public Submission get(UUID submissionId, AuthenticatedUser user) {
        Submission submission = get(submissionId);
        if (user.role() == UserRole.ADMIN) {
            return submission;
        }
        Assignment assignment = assignments.findById(submission.getAssignmentId())
                .orElseThrow(() -> SubmissionException.notFound("Assignment not found"));
        Course course = assignment.getCourseId() == null ? null : courses.course(assignment.getCourseId());
        boolean allowed = user.role() == UserRole.STUDENT && user.userId().equals(submission.getStudentId())
                || user.role() == UserRole.TEACHER && course != null && user.userId().equals(course.getTeacherId());
        if (!allowed) {
            throw new BusinessException(403, "You do not have access to this submission");
        }
        return submission;
    }

    @Transactional(readOnly = true)
    public CompilationResult resultOf(UUID submissionId) {
        return results.findBySubmissionId(submissionId).orElse(null);
    }
}
