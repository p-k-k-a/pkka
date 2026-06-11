ALTER TABLE applications
    ADD COLUMN rejection_reason VARCHAR(1000);

ALTER TABLE applications
    ALTER COLUMN faculty TYPE VARCHAR(20);

ALTER TABLE applications
    ADD CONSTRAINT chk_applications_faculty
        CHECK (faculty IN ('WILGZ', 'WIMIIP', 'WEAIIB', 'WIET', 'WIMIR',
                           'WGGIOS', 'WGGIIS', 'WIMIC', 'WO', 'WMN',
                           'WWNIG', 'WZ', 'WEIP', 'WFIIS', 'WMS',
                           'WH', 'WI', 'WTK'));

ALTER TABLE applications
    ADD COLUMN phone_number VARCHAR(32) NOT NULL;

ALTER TABLE applications
    ADD CONSTRAINT chk_applications_phone_number
        CHECK (phone_number ~ '^\+?[0-9 \-]{7,32}$');

ALTER TABLE applications
    DROP CONSTRAINT chk_applications_meeting_preference;

ALTER TABLE applications
    DROP COLUMN meeting_preference;

DROP TABLE application_interests;

ALTER TABLE applications
    ADD COLUMN interests           VARCHAR(100)[] NOT NULL DEFAULT '{}',
    ADD COLUMN meeting_preferences VARCHAR(20)[]  NOT NULL DEFAULT '{}';

ALTER TABLE applications
    ADD CONSTRAINT chk_applications_meeting_preferences
        CHECK (meeting_preferences <@ ARRAY['ONLINE', 'IN_PERSON_KRAKOW', 'HYBRID']::VARCHAR[]);
