-- Nullable: events created before authorship was tracked have no known author.
ALTER TABLE events
    ADD COLUMN author_id UUID REFERENCES users(id);

CREATE INDEX idx_events_author_id ON events(author_id);
