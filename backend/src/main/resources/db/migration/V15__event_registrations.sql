-- The unique constraint is what makes a double registration impossible even if two concurrent requests
-- slip past the check in the service; its index also serves the per-event seat count.
CREATE TABLE event_registrations (
    id            UUID PRIMARY KEY,
    event_id      UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    user_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    registered_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_event_registrations_event_user UNIQUE (event_id, user_id)
);

-- "Which events am I signed up for" — the other direction.
CREATE INDEX idx_event_registrations_user_id ON event_registrations (user_id);
