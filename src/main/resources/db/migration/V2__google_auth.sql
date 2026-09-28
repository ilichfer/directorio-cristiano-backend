CREATE TYPE auth_provider AS ENUM ('local', 'google', 'google_demo');

ALTER TABLE users
    ALTER COLUMN password_hash DROP NOT NULL;

ALTER TABLE users
    ADD COLUMN auth_provider auth_provider NOT NULL DEFAULT 'local',
    ADD COLUMN google_sub VARCHAR(255) UNIQUE;
