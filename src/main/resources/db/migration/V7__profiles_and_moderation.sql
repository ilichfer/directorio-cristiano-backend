-- Perfiles de cliente/emprendedor y moderación de negocios (specs/002-dual-profiles-approval).
-- Enums como VARCHAR + CHECK, no tipos ENUM nativos (ver V4__fix_enum_columns.sql).

-- Usuarios: aceptación del acuerdo de emprendedor y permiso de moderación.
ALTER TABLE users
    ADD COLUMN entrepreneur_agreement_at TIMESTAMP WITH TIME ZONE NULL,
    ADD COLUMN is_moderator BOOLEAN NOT NULL DEFAULT false;

-- Las cuentas que ya eran emprendedoras aceptaron el acuerdo al registrarse.
UPDATE users SET entrepreneur_agreement_at = created_at WHERE user_type = 'entrepreneur';

-- Negocios: estado de publicación. Los existentes quedan publicados (FR-025)...
ALTER TABLE businesses
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'published'
        CHECK (status IN ('draft', 'in_review', 'published', 'paused', 'suspended')),
    ADD COLUMN published_at TIMESTAMP WITH TIME ZONE NULL,
    ADD COLUMN suspension_reason TEXT NULL;

UPDATE businesses SET published_at = created_at;

-- ...pero cualquier negocio nuevo nace en borrador: nada se publica sin aprobación (principio II).
ALTER TABLE businesses ALTER COLUMN status SET DEFAULT 'draft';

-- Los borradores pueden estar incompletos; la completitud se valida al enviar a revisión.
ALTER TABLE businesses
    ALTER COLUMN contact_phone DROP NOT NULL,
    ALTER COLUMN contact_whatsapp DROP NOT NULL,
    ALTER COLUMN contact_email DROP NOT NULL,
    ALTER COLUMN contact_address DROP NOT NULL;

CREATE INDEX idx_businesses_status ON businesses(status);

-- Solicitudes de cambios: publicación de un negocio nuevo o edición de uno publicado.
CREATE TABLE business_change_requests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    business_id UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    type VARCHAR(20) NOT NULL CHECK (type IN ('new_business', 'update')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('pending', 'approved', 'rejected', 'cancelled')),
    proposed JSONB NOT NULL,
    submitted_by UUID NOT NULL REFERENCES users(id),
    submitted_at TIMESTAMP WITH TIME ZONE NOT NULL,
    reviewed_by UUID NULL REFERENCES users(id),
    reviewed_at TIMESTAMP WITH TIME ZONE NULL,
    rejection_reason TEXT NULL,
    version INT NOT NULL DEFAULT 0,
    CHECK (status <> 'rejected' OR rejection_reason IS NOT NULL)
);

-- Como máximo una solicitud pendiente por negocio (FR-011).
CREATE UNIQUE INDEX uq_change_request_pending
    ON business_change_requests(business_id) WHERE status = 'pending';

CREATE INDEX idx_change_requests_inbox ON business_change_requests(status, submitted_at);

-- Historial inmutable de moderación (FR-024).
CREATE TABLE moderation_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    business_id UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    change_request_id UUID NULL REFERENCES business_change_requests(id) ON DELETE SET NULL,
    actor_id UUID NOT NULL REFERENCES users(id),
    action VARCHAR(20) NOT NULL CHECK (action IN (
        'submitted', 'resubmitted', 'approved', 'rejected', 'cancelled',
        'suspended', 'reactivated', 'paused', 'resumed')),
    reason TEXT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_moderation_events_business ON moderation_events(business_id, created_at DESC);
