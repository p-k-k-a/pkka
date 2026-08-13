-- Dev-only sign-ups, so the "X of Y seats" counters are not all zero. The file name has to sort after
-- R__dev_seed_alumni and R__dev_seed_events.
INSERT INTO event_registrations (id, event_id, user_id, registered_at)
SELECT
    -- Derived from the pair: re-running this repeatable migration must not pile up new rows.
    md5(e.event_id || u.id::text)::uuid,
    e.event_id::uuid,
    u.id,
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
