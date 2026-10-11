CREATE TABLE tickets
(
    id             UUID         NOT NULL,
    category       VARCHAR(32)  NOT NULL,
    title          VARCHAR(300) NOT NULL,
    description    TEXT         NOT NULL,
    status         VARCHAR(32)  NOT NULL DEFAULT 'OPEN',
    admin_response TEXT,
    author_id      UUID         NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_tickets          PRIMARY KEY (id),
    CONSTRAINT fk_tickets_author   FOREIGN KEY (author_id) REFERENCES users (id),
    CONSTRAINT chk_tickets_category CHECK (category IN ('TOPIC_PROPOSAL', 'TECHNICAL_ISSUE', 'OTHER')),
    CONSTRAINT chk_tickets_status   CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'REJECTED'))
);

CREATE INDEX idx_tickets_status_created_at ON tickets (status, created_at DESC);
CREATE INDEX idx_tickets_author_created_at ON tickets (author_id, created_at DESC);
