-- Since when each file in object storage has been unreferenced. The nightly cleanup deletes a file only
-- after it has stayed here for the whole retention period; attaching it again removes the row.
CREATE TABLE storage_orphans (
    object_key    VARCHAR(500) PRIMARY KEY,
    first_seen_at TIMESTAMPTZ  NOT NULL
);

-- Identifies this database to the bucket it cleans. The id and the number of the latest deleting run are
-- written into the bucket; a database with another id, or with a run number that has fallen behind (a copy,
-- a restore from a backup), refuses to clean that bucket.
CREATE TABLE storage_cleanup_owner (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    generation  BIGINT      NOT NULL DEFAULT 0,
    last_run_at TIMESTAMPTZ
);

INSERT INTO storage_cleanup_owner DEFAULT VALUES;
