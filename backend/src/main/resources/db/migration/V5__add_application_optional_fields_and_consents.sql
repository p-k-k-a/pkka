ALTER TABLE applications
    ADD COLUMN meeting_preference      VARCHAR(20),
    ADD COLUMN co_creation_interest    BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN newsletter_subscription BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE applications
    ADD CONSTRAINT chk_applications_meeting_preference
        CHECK (meeting_preference IS NULL OR meeting_preference IN ('ONLINE', 'IN_PERSON_KRAKOW', 'HYBRID'));

CREATE TABLE application_interests
(
    application_id UUID        NOT NULL,
    interest       VARCHAR(40) NOT NULL,

    CONSTRAINT pk_application_interests             PRIMARY KEY (application_id, interest),
    CONSTRAINT fk_application_interests_application FOREIGN KEY (application_id) REFERENCES applications (id) ON DELETE CASCADE,
    CONSTRAINT chk_application_interests_interest   CHECK (interest IN ('FUTURE_TECH', 'CAREER_MENTORING', 'NETWORKING', 'SCIENTIFIC_COLLABORATION'))
);

CREATE INDEX idx_application_interests_application ON application_interests (application_id);

CREATE TABLE application_consents
(
    id             UUID        NOT NULL,
    application_id UUID        NOT NULL,
    type           VARCHAR(40) NOT NULL,
    granted_at     TIMESTAMPTZ NOT NULL,

    CONSTRAINT pk_application_consents             PRIMARY KEY (id),
    CONSTRAINT uq_application_consents             UNIQUE (application_id, type),
    CONSTRAINT fk_application_consents_application FOREIGN KEY (application_id) REFERENCES applications (id) ON DELETE CASCADE,
    CONSTRAINT chk_application_consents_type       CHECK (type IN ('REGULATIONS_PRIVACY', 'GDPR_DATA_PROCESSING'))
);

CREATE INDEX idx_application_consents_application ON application_consents (application_id);
