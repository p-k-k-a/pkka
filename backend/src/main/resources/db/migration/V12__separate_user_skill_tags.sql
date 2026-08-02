-- Event tags (categories like "workshop", "networking") and alumni skill tags (e.g. "Java", "DevOps")
-- are semantically disjoint domains that used to share the `tags` catalog and its global unique-name
-- constraint. This gives alumni skill tags their own catalog table.

CREATE TABLE user_skill_tags (
    id   UUID PRIMARY KEY,
    name VARCHAR(32) NOT NULL,
    CONSTRAINT uk_user_skill_tags_name UNIQUE (name)
);

-- Move the IT skill tag catalog rows originally seeded by V9__user_profile_tags.sql into the new,
-- dedicated catalog, preserving their ids so the existing user_tags FK repointed below keeps working.
INSERT INTO user_skill_tags (id, name)
SELECT id, name FROM tags
WHERE id IN (
    'cccc0001-cccc-cccc-cccc-cccccccccccc',
    'cccc0002-cccc-cccc-cccc-cccccccccccc',
    'cccc0003-cccc-cccc-cccc-cccccccccccc',
    'cccc0004-cccc-cccc-cccc-cccccccccccc',
    'cccc0005-cccc-cccc-cccc-cccccccccccc',
    'cccc0006-cccc-cccc-cccc-cccccccccccc',
    'cccc0007-cccc-cccc-cccc-cccccccccccc',
    'cccc0008-cccc-cccc-cccc-cccccccccccc',
    'cccc0009-cccc-cccc-cccc-cccccccccccc',
    'cccc0010-cccc-cccc-cccc-cccccccccccc',
    'cccc0011-cccc-cccc-cccc-cccccccccccc',
    'cccc0012-cccc-cccc-cccc-cccccccccccc',
    'cccc0013-cccc-cccc-cccc-cccccccccccc',
    'cccc0014-cccc-cccc-cccc-cccccccccccc',
    'cccc0015-cccc-cccc-cccc-cccccccccccc'
);

-- Repoint user_tags.tag_id from the shared `tags` catalog to the new user-only catalog.
ALTER TABLE user_tags DROP CONSTRAINT user_tags_tag_id_fkey;
ALTER TABLE user_tags
    ADD CONSTRAINT user_tags_tag_id_fkey FOREIGN KEY (tag_id) REFERENCES user_skill_tags(id) ON DELETE CASCADE;

-- The rows that were only ever used as skill tags no longer belong in the event-tags catalog.
DELETE FROM tags
WHERE id IN (
    'cccc0001-cccc-cccc-cccc-cccccccccccc',
    'cccc0002-cccc-cccc-cccc-cccccccccccc',
    'cccc0003-cccc-cccc-cccc-cccccccccccc',
    'cccc0004-cccc-cccc-cccc-cccccccccccc',
    'cccc0005-cccc-cccc-cccc-cccccccccccc',
    'cccc0006-cccc-cccc-cccc-cccccccccccc',
    'cccc0007-cccc-cccc-cccc-cccccccccccc',
    'cccc0008-cccc-cccc-cccc-cccccccccccc',
    'cccc0009-cccc-cccc-cccc-cccccccccccc',
    'cccc0010-cccc-cccc-cccc-cccccccccccc',
    'cccc0011-cccc-cccc-cccc-cccccccccccc',
    'cccc0012-cccc-cccc-cccc-cccccccccccc',
    'cccc0013-cccc-cccc-cccc-cccccccccccc',
    'cccc0014-cccc-cccc-cccc-cccccccccccc',
    'cccc0015-cccc-cccc-cccc-cccccccccccc'
);
