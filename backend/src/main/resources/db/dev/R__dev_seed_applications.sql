-- Dev seed: 25 UNDER_REVIEW applications so the admin list pagination is testable
-- (page size 20 → two pages). Each application needs its own user because of
-- uq_applications_active_per_user (one UNDER_REVIEW/APPROVED per applicant).

INSERT INTO users (id, keycloak_id, created_at, updated_at)
SELECT
    ('a0000000-0000-4000-8000-' || lpad(to_hex(g), 12, '0'))::uuid,
    'seed-applicant-' || g,
    now() - make_interval(hours => g),
    now() - make_interval(hours => g)
FROM generate_series(1, 25) AS g
ON CONFLICT (id) DO NOTHING;

INSERT INTO applications (
    id,
    applicant_id,
    status,
    faculty,
    field_of_study,
    study_type,
    graduation_year,
    phone_number,
    interests,
    meeting_preferences,
    co_creation_interest,
    newsletter_subscription,
    created_at,
    updated_at,
    version
)
SELECT
    ('b0000000-0000-4000-8000-' || lpad(to_hex(g), 12, '0'))::uuid,
    ('a0000000-0000-4000-8000-' || lpad(to_hex(g), 12, '0'))::uuid,
    'UNDER_REVIEW',
    (ARRAY['WI', 'WIET', 'WEAIE', 'WE', 'WEGH', 'WEAIIE'])[1 + ((g - 1) % 6)],
    (ARRAY[
        'Informatyka',
        'Informatyka Stosowana',
        'Cyberbezpieczeństwo',
        'Data Science',
        'Sztuczna Inteligencja'
    ])[1 + ((g - 1) % 5)],
    (ARRAY['BACHELOR', 'MASTER', 'DOCTORAL', 'POSTGRADUATE'])[1 + ((g - 1) % 4)],
    2015 + ((g - 1) % 10),
    '+48 600 700 ' || lpad(g::text, 3, '0'),
    ARRAY['AI', 'mentoring']::varchar(100)[],
    ARRAY[
        (ARRAY['ONLINE', 'IN_PERSON_KRAKOW', 'HYBRID'])[1 + ((g - 1) % 3)]
    ]::varchar(20)[],
    (g % 2 = 0),
    (g % 3 = 0),
    now() - make_interval(hours => g),
    now() - make_interval(hours => g),
    0
FROM generate_series(1, 25) AS g
ON CONFLICT (id) DO NOTHING;

INSERT INTO application_consents (id, application_id, type, granted_at)
SELECT
    ('c0000000-0000-4000-8000-' || lpad(to_hex(g * 2 - 1), 12, '0'))::uuid,
    ('b0000000-0000-4000-8000-' || lpad(to_hex(g), 12, '0'))::uuid,
    'REGULATIONS_PRIVACY',
    now() - make_interval(hours => g)
FROM generate_series(1, 25) AS g
ON CONFLICT (id) DO NOTHING;

INSERT INTO application_consents (id, application_id, type, granted_at)
SELECT
    ('c0000000-0000-4000-8000-' || lpad(to_hex(g * 2), 12, '0'))::uuid,
    ('b0000000-0000-4000-8000-' || lpad(to_hex(g), 12, '0'))::uuid,
    'GDPR_DATA_PROCESSING',
    now() - make_interval(hours => g)
FROM generate_series(1, 25) AS g
ON CONFLICT (id) DO NOTHING;
