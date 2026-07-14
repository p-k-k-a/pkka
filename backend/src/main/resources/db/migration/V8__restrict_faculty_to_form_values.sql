-- Faculty now mirrors the recruitment form: historical names of today's
-- Faculty of Computer Science instead of the full AGH faculty list.
ALTER TABLE applications
    DROP CONSTRAINT chk_applications_faculty;

ALTER TABLE applications
    ADD CONSTRAINT chk_applications_faculty
        CHECK (faculty IN ('WE', 'WEGH', 'WEAIE', 'WEAIIE', 'WIET', 'WI'));
