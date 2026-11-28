-- Joins up to this moment were already reported to the coordinator in the daily digest.
ALTER TABLE topic_groups ADD COLUMN last_join_digest_at TIMESTAMPTZ;

-- Members of existing groups are not news.
UPDATE topic_groups SET last_join_digest_at = now();
