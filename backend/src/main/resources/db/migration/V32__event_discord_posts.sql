CREATE TABLE event_discord_posts (
    event_id   UUID        PRIMARY KEY REFERENCES events (id) ON DELETE CASCADE,
    channel_id VARCHAR(32) NOT NULL,
    message_id VARCHAR(32) NOT NULL,
    posted_at  TIMESTAMPTZ NOT NULL
);
