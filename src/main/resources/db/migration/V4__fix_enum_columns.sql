-- Hibernate (@Enumerated(STRING)) envía estos valores como VARCHAR sin cast,
-- lo que Postgres rechaza contra un tipo ENUM nativo ("la columna X es de
-- tipo X pero la expresión es de tipo character varying"). Esto ya afectaba
-- al registro común (user_type, verification_step), no solo a auth_provider.
-- Se simplifica a VARCHAR con los mismos valores permitidos.

ALTER TABLE users ALTER COLUMN user_type DROP DEFAULT;
ALTER TABLE users ALTER COLUMN verification_step DROP DEFAULT;
ALTER TABLE users ALTER COLUMN auth_provider DROP DEFAULT;

ALTER TABLE users ALTER COLUMN user_type TYPE VARCHAR(20) USING user_type::text;
ALTER TABLE users ALTER COLUMN verification_step TYPE VARCHAR(20) USING verification_step::text;
ALTER TABLE users ALTER COLUMN auth_provider TYPE VARCHAR(20) USING auth_provider::text;

ALTER TABLE users ALTER COLUMN user_type SET DEFAULT 'buyer';
ALTER TABLE users ALTER COLUMN verification_step SET DEFAULT 'unverified';
ALTER TABLE users ALTER COLUMN auth_provider SET DEFAULT 'local';

DROP TYPE user_type;
DROP TYPE verification_step;
DROP TYPE auth_provider;
