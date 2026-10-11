-- Event tags (categories like "workshop", "networking") and alumni skill tags (e.g. "Java", "DevOps")
-- are semantically disjoint domains that used to share the `tags` catalog and its global unique-name
-- constraint. This gives alumni skill tags their own catalog table.

CREATE TABLE user_skill_tags (
    id         UUID PRIMARY KEY,
    name       VARCHAR(32) NOT NULL,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL
);

CREATE UNIQUE INDEX uk_user_skill_tags_name ON user_skill_tags (lower(name));

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

-- While the catalog was shared, tag assignments were validated against the whole `tags` table, so
-- user_tags may legally reference event-category tags ("workshop", ...) and event_tags may reference
-- skill tags. Those cross-domain links are meaningless after the split and must go explicitly:
-- otherwise the FK repoint below fails on such user_tags rows, and the final DELETE FROM tags would
-- silently cascade into event_tags (its FK is ON DELETE CASCADE) instead of being a conscious cleanup.
DELETE FROM user_tags
WHERE tag_id NOT IN (SELECT id FROM user_skill_tags);

DELETE FROM event_tags
WHERE tag_id IN (SELECT id FROM user_skill_tags);

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

INSERT INTO user_skill_tags (id, name)
SELECT gen_random_uuid(), name
FROM unnest(ARRAY[
    'C', 'C++', 'C#', 'Rust', 'Swift', 'PHP', 'Ruby', 'Scala', 'SQL', 'R',
    'React', 'React Native', 'Angular', 'Vue', 'Node.js', 'Spring', '.NET', 'Django', 'Android', 'iOS', 'Flutter',
    'AWS', 'Azure', 'GCP', 'Docker', 'Kubernetes', 'Linux', 'CI/CD', 'Networking',
    'Data Science', 'AI', 'Big Data', 'Databases',
    'Fullstack', 'QA', 'UX/UI', 'Product Management', 'Project Management', 'Team Leadership', 'Architecture',
    'Game Development', 'Blockchain', 'IoT', 'Agile', 'Startups', 'Research'
]) AS name;
