-- Un borrador puede guardarse incompleto (FR-008): V7 liberó los contactos, pero rubro, zona y
-- descripción seguían siendo NOT NULL desde V1. La completitud se valida al enviar a revisión.
ALTER TABLE businesses
    ALTER COLUMN category DROP NOT NULL,
    ALTER COLUMN zone DROP NOT NULL,
    ALTER COLUMN description DROP NOT NULL;
