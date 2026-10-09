CREATE TABLE surveys
(
    id          UUID         NOT NULL,
    title       VARCHAR(300) NOT NULL,
    description TEXT,
    ends_at     TIMESTAMPTZ  NOT NULL,
    status      VARCHAR(32)  NOT NULL DEFAULT 'DRAFT',
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_surveys        PRIMARY KEY (id),
    CONSTRAINT chk_surveys_status CHECK (status IN ('DRAFT', 'ACTIVE', 'CLOSED'))
);

CREATE INDEX idx_surveys_status_ends_at ON surveys (status, ends_at);

CREATE TABLE survey_questions
(
    id            UUID         NOT NULL,
    survey_id     UUID         NOT NULL,
    content       TEXT         NOT NULL,
    type          VARCHAR(32)  NOT NULL,
    display_order INT          NOT NULL,

    CONSTRAINT pk_survey_questions        PRIMARY KEY (id),
    CONSTRAINT uq_survey_questions_id_survey UNIQUE (id, survey_id),
    CONSTRAINT fk_survey_questions_survey FOREIGN KEY (survey_id) REFERENCES surveys (id) ON DELETE CASCADE,
    CONSTRAINT chk_survey_questions_type  CHECK (type IN ('SINGLE_CHOICE', 'MULTI_CHOICE', 'TEXT'))
);

CREATE INDEX idx_survey_questions_survey_order ON survey_questions (survey_id, display_order);

CREATE TABLE survey_question_options
(
    id            UUID         NOT NULL,
    question_id   UUID         NOT NULL,
    label         VARCHAR(500) NOT NULL,
    display_order INT          NOT NULL,

    CONSTRAINT pk_survey_question_options        PRIMARY KEY (id),
    CONSTRAINT fk_survey_question_options_question FOREIGN KEY (question_id) REFERENCES survey_questions (id) ON DELETE CASCADE
);

CREATE INDEX idx_survey_question_options_question_order ON survey_question_options (question_id, display_order);

CREATE TABLE survey_submissions
(
    id           UUID        NOT NULL,
    survey_id    UUID        NOT NULL,
    user_id      UUID        NOT NULL,
    submitted_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT pk_survey_submissions        PRIMARY KEY (id),
    CONSTRAINT uq_survey_submissions_id_survey UNIQUE (id, survey_id),
    CONSTRAINT fk_survey_submissions_survey FOREIGN KEY (survey_id) REFERENCES surveys (id),
    CONSTRAINT fk_survey_submissions_user   FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uq_survey_submissions_user_survey UNIQUE (user_id, survey_id)
);

-- Composite unique constraints only enforce column order for equality lookups, so a plain index
-- with survey_id leading is still needed for admin queries like "all submissions for a survey"
-- (uq_survey_submissions_user_survey has user_id first and can't serve that access pattern).
CREATE INDEX idx_survey_submissions_survey_id ON survey_submissions (survey_id);

CREATE TABLE survey_answers
(
    id            UUID NOT NULL,
    submission_id UUID NOT NULL,
    survey_id     UUID NOT NULL,
    question_id   UUID NOT NULL,
    value         TEXT NOT NULL,

    CONSTRAINT pk_survey_answers              PRIMARY KEY (id),
    -- Composite FK against uq_survey_submissions_id_survey: answer.survey_id must match the
    -- submission's survey, not just any submission row.
    CONSTRAINT fk_survey_answers_submission_survey FOREIGN KEY (submission_id, survey_id) REFERENCES survey_submissions (id, survey_id) ON DELETE CASCADE,
    CONSTRAINT fk_survey_answers_survey       FOREIGN KEY (survey_id) REFERENCES surveys (id),
    -- Composite FK against uq_survey_questions_id_survey: answer.survey_id must match the
    -- question's survey too, so question and submission cannot point at different surveys.
    CONSTRAINT fk_survey_answers_question_survey FOREIGN KEY (question_id, survey_id) REFERENCES survey_questions (id, survey_id),
    CONSTRAINT uq_survey_answers_submission_question UNIQUE (submission_id, question_id)
);
