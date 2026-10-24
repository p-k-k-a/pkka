-- Object key of the user's avatar in the private object storage bucket (Cloudflare R2); NULL means no avatar.
-- Clients get a short-lived signed URL for it from the profile API.
ALTER TABLE users
    ADD COLUMN avatar_key VARCHAR(255);
