CREATE TABLE legal_documents
(
    id                    UUID         NOT NULL,
    type                  VARCHAR(40)  NOT NULL,
    version               INTEGER      NOT NULL,
    title                 VARCHAR(200) NOT NULL,
    content               TEXT         NOT NULL,
    change_summary        TEXT,
    requires_reacceptance BOOLEAN      NOT NULL DEFAULT FALSE,
    published_at          TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_legal_documents         PRIMARY KEY (id),
    CONSTRAINT uq_legal_documents_version UNIQUE (type, version),
    CONSTRAINT chk_legal_documents_type   CHECK (type IN ('TERMS', 'PRIVACY_NOTICE', 'MEMBERSHIP_CONSENT', 'NEWSLETTER_CONSENT')),
    CONSTRAINT chk_legal_documents_ver    CHECK (version >= 1)
);

-- Documents are never deleted: a consent record is proof of the exact text that was accepted.
CREATE TABLE consent_records
(
    id           UUID        NOT NULL,
    user_id      UUID        NOT NULL,
    document_id  UUID        NOT NULL,
    source       VARCHAR(20) NOT NULL,
    granted_at   TIMESTAMPTZ NOT NULL,
    withdrawn_at TIMESTAMPTZ,

    CONSTRAINT pk_consent_records          PRIMARY KEY (id),
    CONSTRAINT uq_consent_records          UNIQUE (user_id, document_id),
    CONSTRAINT fk_consent_records_user     FOREIGN KEY (user_id)     REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_consent_records_document FOREIGN KEY (document_id) REFERENCES legal_documents (id),
    CONSTRAINT chk_consent_records_source  CHECK (source IN ('APPLICATION', 'REACCEPTANCE', 'SETTINGS', 'LEGACY')),
    CONSTRAINT chk_consent_records_period  CHECK (withdrawn_at IS NULL OR withdrawn_at >= granted_at)
);

CREATE INDEX idx_consent_records_user ON consent_records (user_id);

-- Version 1 of every document records what the application form asked before documents were
-- versioned, so the consents already given point at the text people actually saw. It is dated no
-- later than the oldest of those consents (LEAST skips NULLs, so either source may be empty).
INSERT INTO legal_documents (id, type, version, title, content, published_at)
SELECT gen_random_uuid(), v.type, 1, v.title, v.content,
       COALESCE(LEAST((SELECT MIN(granted_at) FROM application_consents),
                      (SELECT MIN(created_at) FROM applications WHERE newsletter_subscription)),
                NOW())
FROM (VALUES
    ('TERMS',
     'Regulamin Klubu Alumnów WI AGH (wersja archiwalna)',
     E'Wersja archiwalna. Zgoda zebrana w formularzu wniosku przed wprowadzeniem wersjonowanych dokumentów.\n\nTreść pola w formularzu: „Akceptuję regulamin oraz politykę prywatności”.'),
    ('PRIVACY_NOTICE',
     'Informacja o przetwarzaniu danych osobowych (wersja archiwalna)',
     E'Wersja archiwalna. Potwierdzenie zebrane w formularzu wniosku przed wprowadzeniem wersjonowanych dokumentów.\n\nTreść pola w formularzu: „Akceptuję regulamin oraz politykę prywatności”.'),
    ('MEMBERSHIP_CONSENT',
     'Zgoda na przetwarzanie danych osobowych (wersja archiwalna)',
     E'Wersja archiwalna. Zgoda zebrana w formularzu wniosku przed wprowadzeniem wersjonowanych dokumentów.\n\nTreść pola w formularzu: „Wyrażam zgodę na przetwarzanie moich danych osobowych (RODO)”.'),
    ('NEWSLETTER_CONSENT',
     'Zgoda na newsletter (wersja archiwalna)',
     E'Wersja archiwalna. Zgoda zebrana w formularzu wniosku przed wprowadzeniem wersjonowanych dokumentów.\n\nTreść pola w formularzu: „Chcę zapisać się do newslettera”.')
) AS v (type, title, content);

INSERT INTO consent_records (id, user_id, document_id, source, granted_at)
SELECT gen_random_uuid(), a.applicant_id, d.id, 'LEGACY', MIN(c.granted_at)
FROM application_consents c
JOIN applications a ON a.id = c.application_id
JOIN legal_documents d
  ON d.version = 1
 AND d.type IN (CASE c.type WHEN 'REGULATIONS_PRIVACY' THEN 'TERMS' WHEN 'GDPR_DATA_PROCESSING' THEN 'MEMBERSHIP_CONSENT' END,
                CASE c.type WHEN 'REGULATIONS_PRIVACY' THEN 'PRIVACY_NOTICE' END)
GROUP BY a.applicant_id, d.id;

INSERT INTO consent_records (id, user_id, document_id, source, granted_at)
SELECT gen_random_uuid(), a.applicant_id, d.id, 'LEGACY', MIN(a.created_at)
FROM applications a
JOIN legal_documents d ON d.type = 'NEWSLETTER_CONSENT' AND d.version = 1
WHERE a.newsletter_subscription
GROUP BY a.applicant_id, d.id;
