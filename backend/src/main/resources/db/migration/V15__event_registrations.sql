-- The unique constraint makes a double registration impossible even if the service check is raced.
CREATE TABLE event_registrations (
    id            UUID PRIMARY KEY,
    event_id      UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    user_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    registered_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_event_registrations_event_user UNIQUE (event_id, user_id)
);

CREATE INDEX idx_event_registrations_user_id ON event_registrations (user_id);
