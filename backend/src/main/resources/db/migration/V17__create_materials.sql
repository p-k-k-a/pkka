CREATE TABLE materials
(
    id          UUID         NOT NULL,
    title       VARCHAR(300) NOT NULL,
    description TEXT,
    type        VARCHAR(32)  NOT NULL,
    url         VARCHAR(2000) NOT NULL,
    event_id    UUID,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_materials        PRIMARY KEY (id),
    CONSTRAINT fk_materials_event  FOREIGN KEY (event_id) REFERENCES events (id),
    CONSTRAINT chk_materials_type  CHECK (type IN ('RECORDING', 'PRESENTATION', 'OTHER'))
);

CREATE INDEX idx_materials_type_created_at ON materials (type, created_at DESC);
CREATE INDEX idx_materials_event_id ON materials (event_id);
