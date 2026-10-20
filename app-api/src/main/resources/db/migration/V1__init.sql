CREATE TABLE assignments (
    id         UUID PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    language   VARCHAR(32)  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL
);

CREATE TABLE submissions (
    id            UUID PRIMARY KEY,
    assignment_id UUID        NOT NULL REFERENCES assignments (id),
    source_code   TEXT        NOT NULL,
    status        VARCHAR(20) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_submissions_assignment ON submissions (assignment_id);
CREATE INDEX idx_submissions_status ON submissions (status);

CREATE TABLE compilation_results (
    id            UUID PRIMARY KEY,
    submission_id UUID        NOT NULL REFERENCES submissions (id),
    status        VARCHAR(20) NOT NULL,
    exit_code     INTEGER     NOT NULL,
    stdout        TEXT,
    stderr        TEXT,
    duration_ms   BIGINT      NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_results_submission ON compilation_results (submission_id);

-- Seed one PYTHON assignment so the API is usable end-to-end without an assignment-creation
-- endpoint (out of Fase 1 scope). Frontends submit against this id.
INSERT INTO assignments (id, name, language, created_at)
VALUES ('11111111-1111-1111-1111-111111111111', 'Demo Python Assignment', 'PYTHON', now());
