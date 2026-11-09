ALTER TABLE events ADD COLUMN slug VARCHAR(255);

UPDATE events SET slug = id::text;

ALTER TABLE events ALTER COLUMN slug SET NOT NULL;

CREATE UNIQUE INDEX idx_events_slug ON events (slug) WHERE deleted_at IS NULL;
