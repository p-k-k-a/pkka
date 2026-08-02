-- Dev seed: 30 verified alumni (users + APPROVED applications) so the alumni directory is testable.
-- Graduation years 2008-2024, mixed mentor flags and shared companies. Two rows set show_name = false
-- and so must NOT appear in the directory, leaving 28 listed - still two pages at the default size of 20.

CREATE TEMP TABLE seed_alumni (
    n           INT PRIMARY KEY,
    first_name  VARCHAR(255),
    last_name   VARCHAR(255),
    position    VARCHAR(255),
    company     VARCHAR(255),
    grad_year   INT,
    mentor      BOOLEAN,
    show_name   BOOLEAN,
    tags        VARCHAR(32)[],
    bio         TEXT
) ON COMMIT DROP;

INSERT INTO seed_alumni VALUES
    (1,  'Tomasz',     'Nowak',         'Senior Java Developer',      'Google',           2014, TRUE,  TRUE,  ARRAY['Java','Cloud','Backend'],                   'Pasjonat systemów rozproszonych i architektury cloud-native.'),
    (2,  'Aleksandra', 'Wiśniewska',    'Data Scientist',             'NVIDIA',           2018, TRUE,  TRUE,  ARRAY['Python','Machine Learning'],                'Buduje modele uczenia maszynowego dla systemów wizyjnych.'),
    (3,  'Marek',      'Kowalski',      'Product Manager',            'Allegro',          2016, FALSE, TRUE,  ARRAY['Frontend'],                                 NULL),
    (4,  'Katarzyna',  'Lewandowska',   'DevOps Engineer',            'Comarch',          2019, TRUE,  TRUE,  ARRAY['DevOps','Cloud'],                           'Automatyzuje pipeline''y CI/CD i infrastrukturę chmurową.'),
    (5,  'Piotr',      'Zieliński',     'Frontend Developer',         'ABB',              2021, FALSE, TRUE,  ARRAY['JavaScript','TypeScript','Frontend'],       NULL),
    (6,  'Magdalena',  'Wójcik',        'Machine Learning Engineer',  'Google',           2015, TRUE,  TRUE,  ARRAY['Python','Machine Learning','Cloud'],        'Wdraża modele ML na produkcję w skali globalnej.'),
    (7,  'Jakub',      'Szymański',     'Backend Developer',          'Comarch',          2012, FALSE, TRUE,  ARRAY['Java','Backend'],                           NULL),
    (8,  'Anna',       'Dąbrowska',     'Security Engineer',          'Motorola Solutions', 2017, TRUE, TRUE,  ARRAY['Security','Backend'],                       'Zajmuje się bezpieczeństwem systemów wbudowanych.'),
    (9,  'Michał',     'Kaczmarek',     'Mobile Developer',           'Software Mansion', 2020, TRUE,  TRUE,  ARRAY['Mobile','TypeScript'],                      'React Native od strony natywnej.'),
    (10, 'Karolina',   'Mazur',         'Data Engineer',              'Allegro',          2013, FALSE, TRUE,  ARRAY['Data Engineering','Python'],                NULL),
    (11, 'Rafał',      'Krawczyk',      'Embedded Engineer',          'Nokia',            2010, FALSE, TRUE,  ARRAY['Embedded','Backend'],                       'Firmware dla stacji bazowych.'),
    (12, 'Joanna',     'Piotrowska',    'Engineering Manager',        'Sabre',            2009, TRUE,  TRUE,  ARRAY['Backend','Cloud'],                          'Prowadzi zespoły platformowe.'),
    (13, 'Grzegorz',   'Grabowski',     'Cloud Architect',            'EPAM',             2011, TRUE,  TRUE,  ARRAY['Cloud','DevOps','Java'],                    NULL),
    (14, 'Natalia',    'Pawlak',        'QA Engineer',                'Comarch',          2022, FALSE, TRUE,  ARRAY['Backend'],                                  NULL),
    (15, 'Łukasz',     'Michalski',     'Site Reliability Engineer',  'Google',           2016, TRUE,  TRUE,  ARRAY['DevOps','Cloud','Go'],                      'SRE dla usług o wysokiej dostępności.'),
    (16, 'Agnieszka',  'Nowakowska',    'UX Engineer',                'Netguru',          2023, FALSE, TRUE,  ARRAY['Frontend','JavaScript'],                    NULL),
    (17, 'Paweł',      'Adamczyk',      'Kotlin Developer',           'Allegro',          2019, TRUE,  TRUE,  ARRAY['Kotlin','Mobile'],                          'Android w skali e-commerce.'),
    (18, 'Monika',     'Dudek',         'Research Engineer',          'NVIDIA',           2008, FALSE, TRUE,  ARRAY['Machine Learning','Python'],                NULL),
    (19, 'Krzysztof',  'Zając',         'Platform Engineer',          'ABB',              2024, TRUE,  TRUE,  ARRAY['Go','DevOps'],                              NULL),
    (20, 'Ewa',        'Król',          'Tech Lead',                  'Software Mansion', 2015, TRUE,  TRUE,  ARRAY['TypeScript','Frontend','Mobile'],           'Prowadzi projekty React Native dla klientów z USA.'),
    (21, 'Bartosz',    'Wieczorek',     'Database Engineer',          'Sabre',            2013, FALSE, TRUE,  ARRAY['Data Engineering','Backend'],               NULL),
    (22, 'Justyna',    'Sikora',        'Security Analyst',           'Motorola Solutions', 2021, TRUE, TRUE,  ARRAY['Security'],                                 NULL),
    (23, 'Adam',       'Baran',         'Full Stack Developer',       'Netguru',          2018, FALSE, TRUE,  ARRAY['JavaScript','TypeScript','Backend'],        'Od API po interfejs.'),
    (24, 'Weronika',   'Rutkowska',     'ML Ops Engineer',            'EPAM',             2020, TRUE,  TRUE,  ARRAY['Machine Learning','DevOps','Cloud'],        NULL),
    (25, 'Szymon',     'Ostrowski',     'Systems Programmer',         'Nokia',            2012, FALSE, TRUE,  ARRAY['Embedded','Go'],                            NULL),
    (26, 'Julia',      'Bąk',           'Frontend Architect',         'Google',           2017, TRUE,  TRUE,  ARRAY['Frontend','TypeScript'],                    'Design systems na dużą skalę.'),
    (27, 'Damian',     'Sadowski',      'Backend Developer',          'Comarch',          2023, FALSE, TRUE,  ARRAY['Java','Backend'],                           NULL),
    (28, 'Marta',      'Głowacka',      'Data Analyst',               'Allegro',          2024, TRUE,  TRUE,  ARRAY['Data Engineering','Python'],                NULL),
    (29, 'Ukryty',     'Alumn',         'Principal Engineer',         'Google',           2011, TRUE,  FALSE, ARRAY['Backend','Cloud'],                          'Woli pozostać anonimowy w katalogu.'),
    (30, 'Anonimowa',  'Absolwentka',   NULL,                         NULL,               2022, FALSE, FALSE, ARRAY[]::VARCHAR(32)[],                            NULL);

INSERT INTO users (
    id, keycloak_id, first_name, last_name, email, bio, current_position, company,
    linkedin_url, github_url, discord_id, willing_to_mentor, graduation_year,
    show_name, show_email, show_discord, created_at, updated_at
)
SELECT
    ('d0000000-0000-4000-8000-' || lpad(to_hex(s.n), 12, '0'))::uuid,
    'seed-alumn-' || s.n,
    s.first_name,
    s.last_name,
    'seed.alumn.' || s.n || '@example.com',
    s.bio,
    s.position,
    s.company,
    CASE WHEN s.n % 3 <> 0 THEN 'https://www.linkedin.com/in/seed-alumn-' || s.n END,
    CASE WHEN s.n % 4 <> 0 THEN 'https://github.com/seed-alumn-' || s.n END,
    CASE WHEN s.n % 5 = 0 THEN (100000000000000000 + s.n)::text END,
    s.mentor,
    s.grad_year,
    s.show_name,
    TRUE,
    TRUE,
    now() - make_interval(days => s.n),
    now() - make_interval(days => s.n)
FROM seed_alumni s
ON CONFLICT (id) DO UPDATE SET
    first_name        = EXCLUDED.first_name,
    last_name         = EXCLUDED.last_name,
    email             = EXCLUDED.email,
    bio               = EXCLUDED.bio,
    current_position  = EXCLUDED.current_position,
    company           = EXCLUDED.company,
    linkedin_url      = EXCLUDED.linkedin_url,
    github_url        = EXCLUDED.github_url,
    discord_id        = EXCLUDED.discord_id,
    willing_to_mentor = EXCLUDED.willing_to_mentor,
    graduation_year   = EXCLUDED.graduation_year,
    show_name         = EXCLUDED.show_name,
    updated_at        = now();

INSERT INTO applications (
    id, applicant_id, status, faculty, field_of_study, study_type, graduation_year,
    phone_number, interests, meeting_preferences, co_creation_interest,
    newsletter_subscription, reviewed_at, created_at, updated_at, version
)
SELECT
    ('e0000000-0000-4000-8000-' || lpad(to_hex(s.n), 12, '0'))::uuid,
    ('d0000000-0000-4000-8000-' || lpad(to_hex(s.n), 12, '0'))::uuid,
    'APPROVED',
    (ARRAY['WI', 'WIET', 'WEAIE', 'WE', 'WEGH', 'WEAIIE'])[1 + ((s.n - 1) % 6)],
    (ARRAY[
        'Informatyka',
        'Informatyka Stosowana',
        'Cyberbezpieczeństwo',
        'Data Science',
        'Sztuczna Inteligencja'
    ])[1 + ((s.n - 1) % 5)],
    (ARRAY['BACHELOR', 'MASTER', 'DOCTORAL', 'POSTGRADUATE'])[1 + ((s.n - 1) % 4)],
    s.grad_year,
    '+48 500 600 ' || lpad(s.n::text, 3, '0'),
    ARRAY['AI', 'mentoring']::varchar(100)[],
    ARRAY[(ARRAY['ONLINE', 'IN_PERSON_KRAKOW', 'HYBRID'])[1 + ((s.n - 1) % 3)]]::varchar(20)[],
    (s.n % 2 = 0),
    (s.n % 3 = 0),
    now() - make_interval(days => s.n * 20),
    now() - make_interval(days => s.n + 400),
    now() - make_interval(days => s.n * 20),
    0
FROM seed_alumni s
ON CONFLICT (id) DO UPDATE SET
    graduation_year = EXCLUDED.graduation_year,
    reviewed_at     = EXCLUDED.reviewed_at,
    updated_at      = now();

INSERT INTO application_consents (id, application_id, type, granted_at)
SELECT
    ('f0000000-0000-4000-8000-' || lpad(to_hex(s.n * 2 - 1), 12, '0'))::uuid,
    ('e0000000-0000-4000-8000-' || lpad(to_hex(s.n), 12, '0'))::uuid,
    'REGULATIONS_PRIVACY',
    now() - make_interval(days => s.n + 400)
FROM seed_alumni s
ON CONFLICT (id) DO NOTHING;

INSERT INTO application_consents (id, application_id, type, granted_at)
SELECT
    ('f0000000-0000-4000-8000-' || lpad(to_hex(s.n * 2), 12, '0'))::uuid,
    ('e0000000-0000-4000-8000-' || lpad(to_hex(s.n), 12, '0'))::uuid,
    'GDPR_DATA_PROCESSING',
    now() - make_interval(days => s.n + 400)
FROM seed_alumni s
ON CONFLICT (id) DO NOTHING;

DELETE FROM user_tags
WHERE user_id IN (SELECT ('d0000000-0000-4000-8000-' || lpad(to_hex(n), 12, '0'))::uuid FROM seed_alumni);

INSERT INTO user_tags (user_id, tag_id)
SELECT
    ('d0000000-0000-4000-8000-' || lpad(to_hex(s.n), 12, '0'))::uuid,
    t.id
FROM seed_alumni s
JOIN user_skill_tags t ON t.name = ANY (s.tags)
ON CONFLICT DO NOTHING;
