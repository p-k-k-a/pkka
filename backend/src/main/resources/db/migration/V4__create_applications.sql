CREATE TABLE applications
(
    id              UUID         NOT NULL,
    applicant_id    UUID         NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'UNDER_REVIEW',
    faculty         VARCHAR(200) NOT NULL,
    field_of_study  VARCHAR(200) NOT NULL,
    study_type      VARCHAR(20)  NOT NULL,
    graduation_year INTEGER      NOT NULL,
    reviewed_by_id  UUID,
    reviewed_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    version         BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT pk_applications             PRIMARY KEY (id),
    CONSTRAINT fk_applications_applicant   FOREIGN KEY (applicant_id)   REFERENCES users (id),
    CONSTRAINT fk_applications_reviewed_by FOREIGN KEY (reviewed_by_id) REFERENCES users (id),
    CONSTRAINT chk_applications_status     CHECK (status IN ('UNDER_REVIEW', 'APPROVED', 'REJECTED', 'WITHDRAWN')),
    CONSTRAINT chk_applications_study_type CHECK (study_type IN ('BACHELOR', 'MASTER', 'DOCTORAL', 'POSTGRADUATE')),
    CONSTRAINT chk_applications_grad_year  CHECK (graduation_year >= 1919)
);

CREATE UNIQUE INDEX uq_applications_open_per_user
    ON applications (applicant_id)
    WHERE status = 'UNDER_REVIEW';

CREATE INDEX idx_applications_status ON applications (status, created_at DESC);

CREATE INDEX idx_applications_applicant ON applications (applicant_id);
