-- Cover of a post: an external http(s) URL or the object key of a file in the private object storage
-- bucket (Cloudflare R2); NULL means no cover. Clients get a short-lived signed URL for a key.
ALTER TABLE posts
    ADD COLUMN cover_image_url VARCHAR(500);
