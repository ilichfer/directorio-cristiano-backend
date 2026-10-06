package com.directoriocristiano.service.notify;

import com.directoriocristiano.model.entity.Business;
import com.directoriocristiano.model.entity.BusinessChangeRequest;

/**
 * Avisos al emprendedor sobre decisiones de moderación (FR-023). Sin SMTP configurado se usan
 * solo el log y el panel; con {@code app.mail.enabled=true}, también correo (research R7).
 */
public interface ModerationNotifier {
    void approved(BusinessChangeRequest request);
    void rejected(BusinessChangeRequest request);
    void suspended(Business business);
}
