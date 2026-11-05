ALTER TABLE applications
    DROP CONSTRAINT fk_applications_applicant,
    DROP CONSTRAINT fk_applications_reviewed_by,
    ADD CONSTRAINT fk_applications_applicant
        FOREIGN KEY (applicant_id) REFERENCES users (id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_applications_reviewed_by
        FOREIGN KEY (reviewed_by_id) REFERENCES users (id) ON DELETE SET NULL;

ALTER TABLE tickets
    DROP CONSTRAINT fk_tickets_author,
    DROP CONSTRAINT fk_tickets_responded_by,
    ADD CONSTRAINT fk_tickets_author
        FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_tickets_responded_by
        FOREIGN KEY (responded_by_id) REFERENCES users (id) ON DELETE SET NULL;

ALTER TABLE survey_submissions
    DROP CONSTRAINT fk_survey_submissions_user,
    ADD CONSTRAINT fk_survey_submissions_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE posts
    ALTER COLUMN author_id DROP NOT NULL,
    DROP CONSTRAINT fk_posts_author,
    ADD CONSTRAINT fk_posts_author
        FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE SET NULL;

ALTER TABLE events
    DROP CONSTRAINT events_author_id_fkey,
    ADD CONSTRAINT fk_events_author
        FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE SET NULL;
