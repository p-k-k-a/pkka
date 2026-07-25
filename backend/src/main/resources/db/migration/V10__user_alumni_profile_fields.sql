-- Extend users table with alumni profile fields (bio, Keycloak-sourced names)
ALTER TABLE users
    ADD COLUMN first_name VARCHAR(255),
    ADD COLUMN last_name  VARCHAR(255),
    ADD COLUMN bio        TEXT;
