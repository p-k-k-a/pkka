CREATE TABLE topic_groups
(
    id          UUID         NOT NULL,
    slug        VARCHAR(255) NOT NULL,
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    owner_id    UUID         NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    version     BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT pk_topic_groups       PRIMARY KEY (id),
    CONSTRAINT uq_topic_groups_slug  UNIQUE (slug),
    CONSTRAINT uq_topic_groups_name  UNIQUE (name),
    CONSTRAINT fk_topic_groups_owner FOREIGN KEY (owner_id) REFERENCES users (id)
);

CREATE INDEX idx_topic_groups_owner ON topic_groups (owner_id);

CREATE TABLE topic_group_requests
(
    id               UUID          NOT NULL,
    requester_id     UUID          NOT NULL,
    proposed_name    VARCHAR(200)  NOT NULL,
    description      TEXT          NOT NULL,
    status           VARCHAR(20)   NOT NULL DEFAULT 'UNDER_REVIEW',
    reviewed_by_id   UUID,
    reviewed_at      TIMESTAMPTZ,
    rejection_reason VARCHAR(1000),
    created_group_id UUID,
    created_at       TIMESTAMPTZ   NOT NULL,
    updated_at       TIMESTAMPTZ   NOT NULL,
    version          BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT pk_topic_group_requests               PRIMARY KEY (id),
    CONSTRAINT fk_topic_group_requests_requester     FOREIGN KEY (requester_id)     REFERENCES users (id),
    CONSTRAINT fk_topic_group_requests_reviewed_by   FOREIGN KEY (reviewed_by_id)   REFERENCES users (id),
    CONSTRAINT fk_topic_group_requests_created_group FOREIGN KEY (created_group_id) REFERENCES topic_groups (id),
    CONSTRAINT chk_topic_group_requests_status       CHECK (status IN ('UNDER_REVIEW', 'APPROVED', 'REJECTED'))
);

-- At most one request awaiting review per person, the way applications do it.
CREATE UNIQUE INDEX uq_topic_group_requests_open_per_user
    ON topic_group_requests (requester_id)
    WHERE status = 'UNDER_REVIEW';

CREATE INDEX idx_topic_group_requests_status ON topic_group_requests (status, created_at DESC);
