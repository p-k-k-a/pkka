CREATE TABLE tags (
                      id   UUID PRIMARY KEY,
                      name VARCHAR(32) NOT NULL,
                      CONSTRAINT uk_tags_name UNIQUE (name)
);

CREATE TABLE events (
                        id                      UUID PRIMARY KEY,
                        title                   VARCHAR(200) NOT NULL,
                        full_description        TEXT,
                        type                    VARCHAR(32) NOT NULL,
                        starts_at               TIMESTAMP NOT NULL,
                        ends_at                 TIMESTAMP NOT NULL,
                        transmission_url        VARCHAR(500),
                        location                VARCHAR(300),
                        seat_limit              INTEGER,
                        registration_closes_at  TIMESTAMP,
                        audience                VARCHAR(32) NOT NULL,
                        cover_image_url         VARCHAR(500),
                        created_at              TIMESTAMP NOT NULL,
                        updated_at              TIMESTAMP NOT NULL,
                        deleted_at              TIMESTAMP,
                        version                 BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_events_starts_at ON events(starts_at);

CREATE TABLE event_tags (
                            event_id UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
                            tag_id   UUID NOT NULL REFERENCES tags(id)   ON DELETE CASCADE,
                            PRIMARY KEY (event_id, tag_id)
);

CREATE INDEX idx_event_tags_tag_id ON event_tags(tag_id);
