CREATE TABLE topic_proposals
(
    id          UUID         NOT NULL,
    title       VARCHAR(300) NOT NULL,
    description TEXT         NOT NULL,
    rationale   TEXT         NOT NULL,
    author_id   UUID         NOT NULL,
    status      VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_topic_proposals         PRIMARY KEY (id),
    CONSTRAINT fk_topic_proposals_author  FOREIGN KEY (author_id) REFERENCES users (id),
    CONSTRAINT chk_topic_proposals_status CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED'))
);

CREATE INDEX idx_topic_proposals_status_created_at ON topic_proposals (status, created_at DESC);
CREATE INDEX idx_topic_proposals_author_created_at ON topic_proposals (author_id, created_at DESC);
