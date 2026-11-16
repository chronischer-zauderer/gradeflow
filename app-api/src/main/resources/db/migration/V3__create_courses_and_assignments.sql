CREATE TABLE courses (
    id          UUID PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    invite_code VARCHAR(8) NOT NULL UNIQUE,
    teacher_id  UUID NOT NULL REFERENCES users (id),
    created_at  TIMESTAMPTZ NOT NULL
);

CREATE TABLE course_enrollments (
    id          UUID PRIMARY KEY,
    course_id   UUID NOT NULL REFERENCES courses (id),
    student_id  UUID NOT NULL REFERENCES users (id),
    enrolled_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_course_student UNIQUE (course_id, student_id)
);

ALTER TABLE assignments RENAME COLUMN name TO title;
ALTER TABLE assignments ADD COLUMN description TEXT NOT NULL DEFAULT '';
ALTER TABLE assignments ADD COLUMN course_id UUID REFERENCES courses (id);
ALTER TABLE assignments ADD COLUMN due_date TIMESTAMPTZ;

ALTER TABLE submissions ADD COLUMN student_id UUID REFERENCES users (id);

CREATE INDEX idx_courses_teacher ON courses (teacher_id);
CREATE INDEX idx_enrollments_student ON course_enrollments (student_id);
CREATE INDEX idx_assignments_course ON assignments (course_id);
CREATE INDEX idx_submissions_student ON submissions (student_id);
