CREATE TABLE tags (
                      id         UUID PRIMARY KEY,
                      name       VARCHAR(32) NOT NULL,
                      created_by UUID REFERENCES users(id) ON DELETE SET NULL
);

CREATE UNIQUE INDEX uk_tags_name ON tags (lower(name));

INSERT INTO tags (id, name) VALUES
    ('11111111-1111-1111-1111-111111111101', 'networking'),
    ('11111111-1111-1111-1111-111111111102', 'technology'),
    ('11111111-1111-1111-1111-111111111103', 'career'),
    ('11111111-1111-1111-1111-111111111104', 'social'),
    ('11111111-1111-1111-1111-111111111105', 'workshop'),
    ('11111111-1111-1111-1111-111111111106', 'ai'),
    ('11111111-1111-1111-1111-111111111107', 'startup'),
    ('11111111-1111-1111-1111-111111111108', 'conference'),
    ('11111111-1111-1111-1111-111111111109', 'alumni'),
    ('11111111-1111-1111-1111-111111111110', 'online');

CREATE TABLE events (
                        id                      UUID PRIMARY KEY,
                        title                   VARCHAR(200) NOT NULL,
                        full_description        TEXT,
                        type                    VARCHAR(32) NOT NULL,
                        starts_at               TIMESTAMPTZ NOT NULL,
                        ends_at                 TIMESTAMPTZ NOT NULL,
                        transmission_url        VARCHAR(500),
                        location                VARCHAR(300),
                        seat_limit              INTEGER,
                        registration_closes_at  TIMESTAMPTZ,
                        audience                VARCHAR(32) NOT NULL,
                        cover_image_url         VARCHAR(500),
                        created_at              TIMESTAMPTZ NOT NULL,
                        updated_at              TIMESTAMPTZ NOT NULL,
                        deleted_at              TIMESTAMPTZ,
                        version                 BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_events_starts_at ON events(starts_at);

CREATE TABLE event_tags (
                            event_id UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
                            tag_id   UUID NOT NULL REFERENCES tags(id)   ON DELETE CASCADE,
                            PRIMARY KEY (event_id, tag_id)
);

CREATE INDEX idx_event_tags_tag_id ON event_tags(tag_id);
