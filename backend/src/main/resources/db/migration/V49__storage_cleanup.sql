-- Since when each file in object storage has been unreferenced. The nightly cleanup deletes a file only
-- after it has stayed here for the whole retention period; attaching it again removes the row.
CREATE TABLE storage_orphans (
    object_key    VARCHAR(500) PRIMARY KEY,
    first_seen_at TIMESTAMPTZ  NOT NULL
);

-- Identifies this database to the bucket it cleans. The id is written into the bucket on the first cleanup,
-- and a database with a different id refuses to clean that bucket. A restore from a backup keeps the id.
CREATE TABLE storage_cleanup_owner (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid()
);

INSERT INTO storage_cleanup_owner DEFAULT VALUES;
