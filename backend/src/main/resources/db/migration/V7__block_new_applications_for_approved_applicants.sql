-- An approved (verified) applicant must not submit a new application,
-- so the one-active-application-per-user guarantee now covers APPROVED too.
DROP INDEX uq_applications_open_per_user;

CREATE UNIQUE INDEX uq_applications_active_per_user
    ON applications (applicant_id)
    WHERE status IN ('UNDER_REVIEW', 'APPROVED');
