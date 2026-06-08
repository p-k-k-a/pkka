CREATE TABLE posts
(
    id           UUID         NOT NULL,
    slug         VARCHAR(300) NOT NULL,
    title        VARCHAR(300) NOT NULL,
    content      TEXT         NOT NULL,
    author_id    UUID         NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    published_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_posts              PRIMARY KEY (id),
    CONSTRAINT uq_posts_slug         UNIQUE (slug),
    CONSTRAINT fk_posts_author       FOREIGN KEY (author_id) REFERENCES users (id),
    CONSTRAINT chk_posts_status      CHECK (status IN ('DRAFT', 'PUBLISHED'))
);

CREATE INDEX idx_posts_status_published_at ON posts (status, published_at DESC);