-- Who may fill a survey in. Existing surveys keep the only audience they could have had.
ALTER TABLE surveys
    ADD COLUMN audience VARCHAR(32) NOT NULL DEFAULT 'VERIFIED_ALUMNI',
    ADD CONSTRAINT chk_surveys_audience CHECK (audience IN ('ALL_USERS', 'VERIFIED_ALUMNI'));

-- A grid question asks the same choice for every row: its options are the columns.
ALTER TABLE survey_questions
    DROP CONSTRAINT chk_survey_questions_type,
    ADD CONSTRAINT chk_survey_questions_type CHECK (type IN ('SINGLE_CHOICE', 'MULTI_CHOICE', 'TEXT', 'GRID'));

CREATE TABLE survey_question_rows
(
    id            UUID         NOT NULL,
    question_id   UUID         NOT NULL,
    label         VARCHAR(500) NOT NULL,
    display_order INT          NOT NULL,

    CONSTRAINT pk_survey_question_rows          PRIMARY KEY (id),
    CONSTRAINT fk_survey_question_rows_question FOREIGN KEY (question_id) REFERENCES survey_questions (id) ON DELETE CASCADE
);

CREATE INDEX idx_survey_question_rows_question_order ON survey_question_rows (question_id, display_order);
