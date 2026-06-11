-- Extend users table with optional profile fields
ALTER TABLE users
    ADD COLUMN current_position VARCHAR(255),
    ADD COLUMN company          VARCHAR(255),
    ADD COLUMN linkedin_url     VARCHAR(500),
    ADD COLUMN github_url       VARCHAR(500);

-- User ↔ Tag many-to-many join table
CREATE TABLE user_tags (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    tag_id  UUID NOT NULL REFERENCES tags(id)  ON DELETE CASCADE,
    PRIMARY KEY (user_id, tag_id)
);

CREATE INDEX idx_user_tags_tag_id ON user_tags(tag_id);

-- Predefined IT skill tags (system-managed, available in all environments)
INSERT INTO tags (id, name) VALUES
    ('cccc0001-cccc-cccc-cccc-cccccccccccc', 'Java'),
    ('cccc0002-cccc-cccc-cccc-cccccccccccc', 'Python'),
    ('cccc0003-cccc-cccc-cccc-cccccccccccc', 'JavaScript'),
    ('cccc0004-cccc-cccc-cccc-cccccccccccc', 'TypeScript'),
    ('cccc0005-cccc-cccc-cccc-cccccccccccc', 'Kotlin'),
    ('cccc0006-cccc-cccc-cccc-cccccccccccc', 'Go'),
    ('cccc0007-cccc-cccc-cccc-cccccccccccc', 'DevOps'),
    ('cccc0008-cccc-cccc-cccc-cccccccccccc', 'Frontend'),
    ('cccc0009-cccc-cccc-cccc-cccccccccccc', 'Backend'),
    ('cccc0010-cccc-cccc-cccc-cccccccccccc', 'Cloud'),
    ('cccc0011-cccc-cccc-cccc-cccccccccccc', 'Machine Learning'),
    ('cccc0012-cccc-cccc-cccc-cccccccccccc', 'Mobile'),
    ('cccc0013-cccc-cccc-cccc-cccccccccccc', 'Data Engineering'),
    ('cccc0014-cccc-cccc-cccc-cccccccccccc', 'Security'),
    ('cccc0015-cccc-cccc-cccc-cccccccccccc', 'Embedded')
ON CONFLICT (name) DO NOTHING;
