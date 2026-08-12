-- "Willing to mentor" is a first-class boolean, not a skill tag: it is not a technology/competency
-- like the entries in user_skill_tags, it is a fixed yes/no filter used directly by the alumni directory.
ALTER TABLE users
    ADD COLUMN willing_to_mentor BOOLEAN NOT NULL DEFAULT FALSE;
