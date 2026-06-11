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
ON CONFLICT (id) DO NOTHING;

INSERT INTO events (
    id, title, full_description, type, starts_at, ends_at, transmission_url,
    location, seat_limit, registration_closes_at, audience, cover_image_url,
    created_at, updated_at, version
) VALUES
    -- 1) ONLINE / PUBLIC / +3d — has a transmission URL, no physical location
    ('22222222-2222-2222-2222-222222222201',
     'Warsztat: Wprowadzenie do AI',
     'Praktyczny warsztat online o podstawach uczenia maszynowego i pracy z modelami językowymi. Dla każdego, kto chce zacząć przygodę z AI.',
     'ONLINE',
     date_trunc('day', now()) + interval '3 days' + interval '17 hours',
     date_trunc('day', now()) + interval '3 days' + interval '19 hours',
     'https://meet.example.com/ai-intro', NULL,
     100, date_trunc('day', now()) + interval '2 days' + interval '23 hours',
     'PUBLIC', 'https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=800&h=450&fit=crop',
     now(), now(), 0),

    -- 2) IN_PERSON / ALL_ALUMNI / +7d — alumni-only, has a venue, no transmission URL
    ('22222222-2222-2222-2222-222222222202',
     'Wieczór networkingowy alumnów',
     'Spotkanie integracyjne absolwentów. Krótkie prezentacje, rozmowy przy kawie i okazja do nawiązania nowych kontaktów zawodowych.',
     'IN_PERSON',
     date_trunc('day', now()) + interval '7 days' + interval '18 hours',
     date_trunc('day', now()) + interval '7 days' + interval '21 hours',
     NULL, 'Kraków, ul. Reymonta 19, sala A',
     80, date_trunc('day', now()) + interval '6 days' + interval '20 hours',
     'ALL_ALUMNI', 'https://images.unsplash.com/photo-1511795409834-ef04bbd61622?w=800&h=450&fit=crop',
     now(), now(), 0),

    -- 3) HYBRID / PUBLIC / +10d — both a venue and a transmission URL; no registration deadline
    ('22222222-2222-2222-2222-222222222203',
     'Tech Talk: Architektura w chmurze',
     'Prelekcja o projektowaniu skalowalnych systemów w chmurze. Udział stacjonarny lub zdalny.',
     'HYBRID',
     date_trunc('day', now()) + interval '10 days' + interval '16 hours',
     date_trunc('day', now()) + interval '10 days' + interval '18 hours',
     'https://meet.example.com/cloud-talk', 'Kraków, AGH, paw. D-17',
     150, NULL,
     'PUBLIC', 'https://images.unsplash.com/photo-1558494949-ef010cbdcc31?w=800&h=450&fit=crop',
     now(), now(), 0),

    -- 4) IN_PERSON / PUBLIC / +14d — large seat limit (300), tests capacity display
    ('22222222-2222-2222-2222-222222222204',
     'Targi Pracy IT 2026',
     'Największe targi pracy dla studentów i absolwentów informatyki. Kilkadziesiąt firm, oferty staży i pracy.',
     'IN_PERSON',
     date_trunc('day', now()) + interval '14 days' + interval '10 hours',
     date_trunc('day', now()) + interval '14 days' + interval '16 hours',
     NULL, 'Kraków, Centrum Kongresowe ICE',
     300, date_trunc('day', now()) + interval '13 days' + interval '23 hours',
     'PUBLIC', 'https://images.unsplash.com/photo-1540575467063-178a50c2df87?w=800&h=450&fit=crop',
     now(), now(), 0),

    -- 5) IN_PERSON / ALL_ALUMNI / +21d — small seat limit (60)
    ('22222222-2222-2222-2222-222222222205',
     'Startup Pitch Night',
     'Wieczór prezentacji startupów założonych przez absolwentów. Jury, nagrody i sesja Q&A z inwestorami.',
     'IN_PERSON',
     date_trunc('day', now()) + interval '21 days' + interval '18 hours',
     date_trunc('day', now()) + interval '21 days' + interval '21 hours',
     NULL, 'Kraków, Cambridge Innovation Center',
     60, date_trunc('day', now()) + interval '20 days' + interval '12 hours',
     'ALL_ALUMNI', 'https://images.unsplash.com/photo-1556761175-5973dc0f32e7?w=800&h=450&fit=crop',
     now(), now(), 0),

    -- 6) HYBRID / PUBLIC / +30d — multi-hour conference, largest seat limit (500)
    ('22222222-2222-2222-2222-222222222206',
     'Konferencja AGH Informatyka',
     'Doroczna konferencja naukowo-techniczna. Wykłady plenarne, sesje tematyczne i panel dyskusyjny.',
     'HYBRID',
     date_trunc('day', now()) + interval '30 days' + interval '9 hours',
     date_trunc('day', now()) + interval '30 days' + interval '17 hours',
     'https://meet.example.com/agh-conf', 'Kraków, AGH, Aula Główna',
     500, date_trunc('day', now()) + interval '28 days' + interval '23 hours',
     'PUBLIC', 'https://images.unsplash.com/photo-1505373877841-8d25f7d46678?w=800&h=450&fit=crop',
     now(), now(), 0),

    -- 7) ONLINE / ALL_ALUMNI / +5d — no seat limit and no registration deadline (open to all)
    ('22222222-2222-2222-2222-222222222207',
     'Sesja Q&A z zarządem klubu',
     'Otwarta sesja pytań i odpowiedzi z zarządem klubu alumnów. Bez limitu miejsc, bez zapisów.',
     'ONLINE',
     date_trunc('day', now()) + interval '5 days' + interval '20 hours',
     date_trunc('day', now()) + interval '5 days' + interval '21 hours',
     'https://meet.example.com/town-hall', NULL,
     NULL, NULL,
     'ALL_ALUMNI', 'https://images.unsplash.com/photo-1543269865-cbf427effbad?w=800&h=450&fit=crop',
     now(), now(), 0),

    -- 8) IN_PERSON / PUBLIC / +12d — small casual meetup, seat limit (40)
    ('22222222-2222-2222-2222-222222222208',
     'Wieczór planszówek',
     'Luźne spotkanie integracyjne przy grach planszowych. Snacki i napoje zapewnione.',
     'IN_PERSON',
     date_trunc('day', now()) + interval '12 days' + interval '18 hours',
     date_trunc('day', now()) + interval '12 days' + interval '22 hours',
     NULL, 'Kraków, klubokawiarnia "Bit"',
     40, date_trunc('day', now()) + interval '11 days' + interval '20 hours',
     'PUBLIC', 'https://images.unsplash.com/photo-1610890716171-6b1bb98ffd09?w=800&h=450&fit=crop',
     now(), now(), 0),

    -- 9) ONLINE / PUBLIC / +45d — far-future event, tests longer date ranges
    ('22222222-2222-2222-2222-222222222209',
     'Bootcamp: Machine Learning od zera',
     'Czterotygodniowy cykl warsztatów online z uczenia maszynowego. Zaczynamy od podstaw, kończymy na wdrożeniu modelu.',
     'ONLINE',
     date_trunc('day', now()) + interval '45 days' + interval '17 hours',
     date_trunc('day', now()) + interval '45 days' + interval '20 hours',
     'https://meet.example.com/ml-bootcamp', NULL,
     200, date_trunc('day', now()) + interval '43 days' + interval '23 hours',
     'PUBLIC', 'https://images.unsplash.com/photo-1498050108023-c5249f4df085?w=800&h=450&fit=crop',
     now(), now(), 0),

    -- 10) IN_PERSON / ALL_ALUMNI / +60d — venue in a different city (Warsaw)
    ('22222222-2222-2222-2222-222222222210',
     'Mentoring Mixer',
     'Spotkanie łączące mentorów-absolwentów ze studentami. Krótkie rozmowy 1:1 w formule speed-mentoringu.',
     'IN_PERSON',
     date_trunc('day', now()) + interval '60 days' + interval '17 hours',
     date_trunc('day', now()) + interval '60 days' + interval '20 hours',
     NULL, 'Warszawa, Centrum Konferencyjne POLIN',
     50, date_trunc('day', now()) + interval '58 days' + interval '23 hours',
     'ALL_ALUMNI', 'https://images.unsplash.com/photo-1517048676732-d65bc937f952?w=800&h=450&fit=crop',
     now(), now(), 0),

    -- 11) IN_PERSON / PUBLIC / +90d — multi-day event (starts_at and ends_at span 24h+)
    ('22222222-2222-2222-2222-222222222211',
     'Hackathon AGH 24h',
     'Całodobowy hackathon zespołowy. Tematy z obszaru AI, fintech i smart city. Nagrody dla najlepszych zespołów.',
     'IN_PERSON',
     date_trunc('day', now()) + interval '90 days' + interval '10 hours',
     date_trunc('day', now()) + interval '91 days' + interval '12 hours',
     NULL, 'Kraków, AGH, paw. C-2',
     120, date_trunc('day', now()) + interval '85 days' + interval '23 hours',
     'PUBLIC', 'https://images.unsplash.com/photo-1504384308090-c894fdcc538d?w=800&h=450&fit=crop',
     now(), now(), 0),

    -- 12) IN_PERSON / SPECIFIC_GROUP / +9d — restricted audience, should be hidden from the public list (visibility test)
    ('22222222-2222-2222-2222-222222222212',
     'Zamknięte spotkanie grupy roboczej',
     'Spotkanie wewnętrzne dedykowanej grupy projektowej. Widoczne tylko dla wybranej grupy.',
     'IN_PERSON',
     date_trunc('day', now()) + interval '9 days' + interval '15 hours',
     date_trunc('day', now()) + interval '9 days' + interval '17 hours',
     NULL, 'Kraków, AGH, sala D-1 305',
     20, date_trunc('day', now()) + interval '8 days' + interval '20 hours',
     'SPECIFIC_GROUP', 'https://images.unsplash.com/photo-1497366811353-6870744d04b2?w=800&h=450&fit=crop',
     now(), now(), 0),

    -- 13) ONLINE / PUBLIC / -7d — PAST event, should not appear in the "upcoming" list (date-filter test)
    ('22222222-2222-2222-2222-222222222213',
     'Webinar: Podsumowanie roku',
     'Archiwalny webinar podsumowujący działalność klubu. Wydarzenie minione — przydatne do testów filtrowania po dacie.',
     'ONLINE',
     date_trunc('day', now()) - interval '7 days' + interval '18 hours',
     date_trunc('day', now()) - interval '7 days' + interval '19 hours',
     'https://meet.example.com/recap', NULL,
     NULL, NULL,
     'PUBLIC', 'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=800&h=450&fit=crop',
     now(), now(), 0),

    -- 14) IN_PERSON / ALL_ALUMNI / -30d — older PAST event, with a registration deadline already elapsed
    ('22222222-2222-2222-2222-222222222214',
     'Gala Absolwentów 2025',
     'Archiwalna gala absolwentów z poprzedniego sezonu. Wydarzenie minione.',
     'IN_PERSON',
     date_trunc('day', now()) - interval '30 days' + interval '19 hours',
     date_trunc('day', now()) - interval '30 days' + interval '23 hours',
     NULL, 'Kraków, Hotel Stary',
     200, date_trunc('day', now()) - interval '32 days' + interval '23 hours',
     'ALL_ALUMNI', 'https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=800&h=450&fit=crop',
     now(), now(), 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO event_tags (event_id, tag_id) VALUES
    -- 1) AI workshop: ai, workshop, online
    ('22222222-2222-2222-2222-222222222201', '11111111-1111-1111-1111-111111111106'),
    ('22222222-2222-2222-2222-222222222201', '11111111-1111-1111-1111-111111111105'),
    ('22222222-2222-2222-2222-222222222201', '11111111-1111-1111-1111-111111111110'),
    -- 2) Networking night: networking, alumni, social
    ('22222222-2222-2222-2222-222222222202', '11111111-1111-1111-1111-111111111101'),
    ('22222222-2222-2222-2222-222222222202', '11111111-1111-1111-1111-111111111109'),
    ('22222222-2222-2222-2222-222222222202', '11111111-1111-1111-1111-111111111104'),
    -- 3) Cloud tech talk: technology
    ('22222222-2222-2222-2222-222222222203', '11111111-1111-1111-1111-111111111102'),
    -- 4) Career fair: career, networking
    ('22222222-2222-2222-2222-222222222204', '11111111-1111-1111-1111-111111111103'),
    ('22222222-2222-2222-2222-222222222204', '11111111-1111-1111-1111-111111111101'),
    -- 5) Startup pitch: startup, technology
    ('22222222-2222-2222-2222-222222222205', '11111111-1111-1111-1111-111111111107'),
    ('22222222-2222-2222-2222-222222222205', '11111111-1111-1111-1111-111111111102'),
    -- 6) Conference: conference, technology
    ('22222222-2222-2222-2222-222222222206', '11111111-1111-1111-1111-111111111108'),
    ('22222222-2222-2222-2222-222222222206', '11111111-1111-1111-1111-111111111102'),
    -- 7) Q&A: alumni
    ('22222222-2222-2222-2222-222222222207', '11111111-1111-1111-1111-111111111109'),
    -- 8) Board games: social
    ('22222222-2222-2222-2222-222222222208', '11111111-1111-1111-1111-111111111104'),
    -- 9) ML bootcamp: ai, workshop, technology
    ('22222222-2222-2222-2222-222222222209', '11111111-1111-1111-1111-111111111106'),
    ('22222222-2222-2222-2222-222222222209', '11111111-1111-1111-1111-111111111105'),
    ('22222222-2222-2222-2222-222222222209', '11111111-1111-1111-1111-111111111102'),
    -- 10) Mentoring: career, networking, alumni
    ('22222222-2222-2222-2222-222222222210', '11111111-1111-1111-1111-111111111103'),
    ('22222222-2222-2222-2222-222222222210', '11111111-1111-1111-1111-111111111101'),
    ('22222222-2222-2222-2222-222222222210', '11111111-1111-1111-1111-111111111109'),
    -- 11) Hackathon: technology, startup, ai
    ('22222222-2222-2222-2222-222222222211', '11111111-1111-1111-1111-111111111102'),
    ('22222222-2222-2222-2222-222222222211', '11111111-1111-1111-1111-111111111107'),
    ('22222222-2222-2222-2222-222222222211', '11111111-1111-1111-1111-111111111106'),
    -- 12) Closed group: alumni
    ('22222222-2222-2222-2222-222222222212', '11111111-1111-1111-1111-111111111109'),
    -- 13) Past webinar: online, alumni
    ('22222222-2222-2222-2222-222222222213', '11111111-1111-1111-1111-111111111110'),
    ('22222222-2222-2222-2222-222222222213', '11111111-1111-1111-1111-111111111109'),
    -- 14) Past gala: social, alumni
    ('22222222-2222-2222-2222-222222222214', '11111111-1111-1111-1111-111111111104'),
    ('22222222-2222-2222-2222-222222222214', '11111111-1111-1111-1111-111111111109')
ON CONFLICT (event_id, tag_id) DO NOTHING;
