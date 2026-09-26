CREATE TABLE push_tickets (
    id         VARCHAR(64)  PRIMARY KEY,
    token      VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_push_tickets_created_at ON push_tickets (created_at);
