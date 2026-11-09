ALTER TABLE tags DROP CONSTRAINT IF EXISTS uk_tags_name;
CREATE UNIQUE INDEX IF NOT EXISTS uk_tags_name ON tags (lower(name));

ALTER TABLE user_skill_tags DROP CONSTRAINT IF EXISTS uk_user_skill_tags_name;
CREATE UNIQUE INDEX IF NOT EXISTS uk_user_skill_tags_name ON user_skill_tags (lower(name));

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
    ('11111111-1111-1111-1111-111111111110', 'online')
ON CONFLICT DO NOTHING;

INSERT INTO user_skill_tags (id, name)
SELECT gen_random_uuid(), name
FROM unnest(ARRAY[
    'C', 'C++', 'C#', 'Rust', 'Swift', 'PHP', 'Ruby', 'Scala', 'SQL', 'R',
    'React', 'React Native', 'Angular', 'Vue', 'Node.js', 'Spring', '.NET', 'Django', 'Android', 'iOS', 'Flutter',
    'AWS', 'Azure', 'GCP', 'Docker', 'Kubernetes', 'Linux', 'CI/CD', 'Networking',
    'Data Science', 'AI', 'Big Data', 'Databases',
    'Fullstack', 'QA', 'UX/UI', 'Product Management', 'Project Management', 'Team Leadership', 'Architecture',
    'Game Development', 'Blockchain', 'IoT', 'Agile', 'Startups', 'Research'
]) AS name
ON CONFLICT DO NOTHING;
