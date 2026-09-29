-- Every row that exists today holds a seat; the waitlist is what this column makes possible.
ALTER TABLE event_registrations
    ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT 'REGISTERED';

ALTER TABLE event_registrations
    ALTER COLUMN status DROP DEFAULT;

-- Serves both hot reads: counting held seats, and finding the head of the queue.
CREATE INDEX idx_event_registrations_event_status
    ON event_registrations (event_id, status, registered_at);
