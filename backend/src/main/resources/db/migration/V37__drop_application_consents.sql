-- Consents live in consent_records since V35; the application form now accepts document versions.
DROP TABLE application_consents;

ALTER TABLE applications
    DROP COLUMN newsletter_subscription;
