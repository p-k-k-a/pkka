-- A Discord member holding the alumn role is approved at login without filling the form,
-- so the form columns may stay empty, but only on applications vouched for by Discord.
ALTER TABLE applications
    ALTER COLUMN faculty DROP NOT NULL,
    ALTER COLUMN field_of_study DROP NOT NULL,
    ALTER COLUMN study_type DROP NOT NULL,
    ALTER COLUMN graduation_year DROP NOT NULL,
    ALTER COLUMN phone_number DROP NOT NULL;

ALTER TABLE applications
    ADD CONSTRAINT chk_applications_form_filled
        CHECK (discord_vouched OR (faculty IS NOT NULL
                                   AND field_of_study IS NOT NULL
                                   AND study_type IS NOT NULL
                                   AND graduation_year IS NOT NULL
                                   AND phone_number IS NOT NULL));
