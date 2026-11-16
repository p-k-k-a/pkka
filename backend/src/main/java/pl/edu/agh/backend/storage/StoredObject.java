package pl.edu.agh.backend.storage;

import java.time.Instant;

/** An object in the bucket, as the cleanup needs to see it. */
record StoredObject(String key, Instant lastModified) {}
