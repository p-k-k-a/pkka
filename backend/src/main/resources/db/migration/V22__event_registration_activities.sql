CREATE TABLE event_registration_activities (
    id          UUID PRIMARY KEY,
    event_id    UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    user_id     UUID REFERENCES users(id) ON DELETE SET NULL,
    type        VARCHAR(32) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_event_registration_activities_event_occurred
    ON event_registration_activities (event_id, occurred_at);

-- Cancellations made before this table existed left no trace; only the sign-ups still standing can be recovered.
INSERT INTO event_registration_activities (id, event_id, user_id, type, occurred_at)
SELECT gen_random_uuid(),
       event_id,
       user_id,
       CASE status WHEN 'WAITLISTED' THEN 'WAITLISTED' ELSE 'SIGNED_UP' END,
       registered_at
FROM event_registrations;
