-- Denormalized from the approved application so the alumni directory can sort and range-filter on it.
ALTER TABLE users
    ADD COLUMN graduation_year INTEGER;

UPDATE users u
SET graduation_year = a.graduation_year
FROM applications a
WHERE a.applicant_id = u.id
  AND a.status = 'APPROVED';

CREATE INDEX idx_users_graduation_year ON users (graduation_year);
