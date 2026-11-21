CREATE TABLE topic_group_memberships
(
    id        UUID        NOT NULL,
    group_id  UUID        NOT NULL,
    user_id   UUID        NOT NULL,
    joined_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT pk_topic_group_memberships       PRIMARY KEY (id),
    CONSTRAINT uq_topic_group_memberships       UNIQUE (group_id, user_id),
    CONSTRAINT fk_topic_group_memberships_group FOREIGN KEY (group_id) REFERENCES topic_groups (id) ON DELETE CASCADE,
    CONSTRAINT fk_topic_group_memberships_user  FOREIGN KEY (user_id)  REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_topic_group_memberships_user ON topic_group_memberships (user_id);

INSERT INTO topic_group_memberships (id, group_id, user_id, joined_at)
SELECT gen_random_uuid(), id, coordinator_id, created_at
FROM topic_groups
WHERE coordinator_id IS NOT NULL;
