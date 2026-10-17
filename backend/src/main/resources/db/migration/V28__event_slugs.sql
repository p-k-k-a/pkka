ALTER TABLE events ADD COLUMN slug VARCHAR(255);

DO $$
DECLARE
    event_row RECORD;
    base TEXT;
    candidate TEXT;
    suffix INT;
BEGIN
    FOR event_row IN
        SELECT id, title FROM events ORDER BY deleted_at IS NOT NULL, created_at, id
    LOOP
        base := lower(translate(
                event_row.title,
                'ąćęłńóśźżĄĆĘŁŃÓŚŹŻáàâäãåéèêëíìîïòôöõúùûüůýÿčďěňřšťžçñÁÀÂÄÃÅÉÈÊËÍÌÎÏÒÔÖÕÚÙÛÜŮÝČĎĚŇŘŠŤŽÇÑ',
                'acelnoszzACELNOSZZaaaaaaeeeeiiiioooouuuuuyycdenrstzcnAAAAAAEEEEIIIIOOOOUUUUUYCDENRSTZCN'));
        base := regexp_replace(base, '[^a-z0-9\s-]', '', 'g');
        base := regexp_replace(base, '\s+', '-', 'g');
        base := regexp_replace(base, '-{2,}', '-', 'g');
        base := regexp_replace(base, '^-+|-+$', '', 'g');
        IF base = '' THEN
            base := 'wydarzenie';
        END IF;

        candidate := base;
        suffix := 2;
        WHILE EXISTS (SELECT 1 FROM events WHERE slug = candidate AND deleted_at IS NULL) LOOP
            candidate := base || '-' || suffix;
            suffix := suffix + 1;
        END LOOP;

        UPDATE events SET slug = candidate WHERE id = event_row.id;
    END LOOP;
END $$;

ALTER TABLE events ALTER COLUMN slug SET NOT NULL;

CREATE UNIQUE INDEX idx_events_slug ON events (slug) WHERE deleted_at IS NULL;
