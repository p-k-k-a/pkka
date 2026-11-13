-- The UPCOMING / PAST event lists split on ends_at (an event stays upcoming until it ends), so the
-- filter needs its own index, as starts_at has.
CREATE INDEX idx_events_ends_at ON events (ends_at);
