-- this is needed for expo push notification service which gives us push_tickets
-- that we later check to see if the notification was successfully delivered
CREATE TABLE push_tickets (
    ticket_id  VARCHAR(64)  PRIMARY KEY,
    token      VARCHAR(255) NOT NULL REFERENCES device_tokens (token) ON DELETE CASCADE,
    created_at TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_push_tickets_created_at ON push_tickets (created_at);
