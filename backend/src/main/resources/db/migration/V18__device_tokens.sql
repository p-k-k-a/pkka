-- One row per app installation. Logout deletes it, so an installation_id is unique at any time.
CREATE TABLE device_tokens (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    installation_id VARCHAR(64)  NOT NULL UNIQUE,
    token           VARCHAR(255) NOT NULL UNIQUE,
    platform        VARCHAR(16)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_device_tokens_user_id ON device_tokens (user_id);
