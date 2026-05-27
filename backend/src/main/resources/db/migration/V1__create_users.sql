CREATE TABLE users (
    id UUID PRIMARY KEY,
    keycloak_id VARCHAR(36) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX idx_users_keycloak_id ON users (keycloak_id);
