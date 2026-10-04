-- Dev-only sign-ups, so the "X of Y seats" counters are not all zero. The file name has to sort after
-- R__dev_seed_alumni and R__dev_seed_events.
INSERT INTO event_registrations (id, event_id, user_id, status, registered_at)
SELECT
    -- Derived from the pair: re-running this repeatable migration must not pile up new rows.
    md5(e.event_id || u.id::text)::uuid,
    e.event_id::uuid,
    u.id,
    'REGISTERED',
    now() - (s.n * interval '3 hours')
FROM (VALUES
    -- 1) AI workshop, PUBLIC, 100 seats
    ('22222222-2222-2222-2222-222222222201', 18),
    -- 2) Networking night, ALL_ALUMNI, 80 seats
    ('22222222-2222-2222-2222-222222222202', 9),
    -- 7) Q&A, ALL_ALUMNI, no seat limit — unlimited events also show a count
    ('22222222-2222-2222-2222-222222222207', 24),
    -- 8) Board games, PUBLIC, 40 seats
    ('22222222-2222-2222-2222-222222222208', 29)
) AS e(event_id, taken)
CROSS JOIN LATERAL generate_series(1, e.taken) AS s(n)
JOIN users u ON u.id = ('d0000000-0000-4000-8000-' || lpad(to_hex(s.n), 12, '0'))::uuid
ON CONFLICT DO NOTHING;

INSERT INTO event_registrations (id, event_id, user_id, status, registered_at)
SELECT
    md5('waitlist' || e.event_id || u.id::text)::uuid,
    e.event_id::uuid,
    u.id,
    'WAITLISTED',
    now() - ((10 - s.n) * interval '20 minutes')
FROM (VALUES
    ('22222222-2222-2222-2222-222222222202', 10, 14)
) AS e(event_id, first_user, last_user)
CROSS JOIN LATERAL generate_series(e.first_user, e.last_user) AS s(n)
JOIN users u ON u.id = ('d0000000-0000-4000-8000-' || lpad(to_hex(s.n), 12, '0'))::uuid
ON CONFLICT DO NOTHING;

-- The statistics screen reads history from this log; seeded sign-ups bypass the service that writes it.
INSERT INTO event_registration_activities (id, event_id, user_id, type, occurred_at)
SELECT md5('activity' || r.id::text)::uuid,
       r.event_id,
       r.user_id,
       CASE r.status WHEN 'WAITLISTED' THEN 'WAITLISTED' ELSE 'SIGNED_UP' END,
       r.registered_at
FROM event_registrations r
WHERE r.event_id IN (
    '22222222-2222-2222-2222-222222222201', '22222222-2222-2222-2222-222222222202',
    '22222222-2222-2222-2222-222222222207', '22222222-2222-2222-2222-222222222208')
ON CONFLICT DO NOTHING;

-- People who signed up for the AI workshop and later dropped out, so cancellations show up per day.
INSERT INTO event_registration_activities (id, event_id, user_id, type, occurred_at)
SELECT md5(a.kind || '22222222-2222-2222-2222-222222222201' || s.n)::uuid,
       '22222222-2222-2222-2222-222222222201'::uuid,
       ('d0000000-0000-4000-8000-' || lpad(to_hex(s.n), 12, '0'))::uuid,
       a.kind,
       now() - ((26 - s.n) * interval '1 day') + a.later
FROM generate_series(19, 24) AS s(n)
CROSS JOIN (VALUES ('SIGNED_UP', interval '0 hours'), ('CANCELLED', interval '1 day 3 hours')) AS a(kind, later)
JOIN users u ON u.id = ('d0000000-0000-4000-8000-' || lpad(to_hex(s.n), 12, '0'))::uuid
ON CONFLICT DO NOTHING;
