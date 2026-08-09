-- Denormalized from the approved application so the alumni directory can sort and range-filter on it.
ALTER TABLE users
    ADD COLUMN graduation_year INTEGER;

-- DISTINCT ON keeps the backfill deterministic when a user somehow has more than one approved
-- application: the most recently reviewed one wins, matching how the profile endpoint resolves
-- education facts (findFirst...OrderByReviewedAtDesc).
UPDATE users u
SET graduation_year = a.graduation_year
FROM (
    SELECT DISTINCT ON (applicant_id) applicant_id, graduation_year
    FROM applications
    WHERE status = 'APPROVED'
    ORDER BY applicant_id, reviewed_at DESC
) a
WHERE a.applicant_id = u.id;

CREATE INDEX idx_users_graduation_year ON users (graduation_year);
