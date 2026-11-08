CREATE TABLE contacts (
    id         UUID PRIMARY KEY,
    owner_id   UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    contact_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_contacts_owner_contact UNIQUE (owner_id, contact_id),
    CONSTRAINT ck_contacts_not_self CHECK (owner_id <> contact_id)
);
