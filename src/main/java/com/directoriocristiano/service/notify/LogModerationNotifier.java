package com.directoriocristiano.service.notify;

import com.directoriocristiano.model.entity.Business;
import com.directoriocristiano.model.entity.BusinessChangeRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LogModerationNotifier implements ModerationNotifier {

    @Override
    public void approved(BusinessChangeRequest request) {
        log.info("Aviso: solicitud {} aprobada para el negocio '{}'",
                request.getId(), request.getBusiness().getName());
    }

    @Override
    public void rejected(BusinessChangeRequest request) {
        log.info("Aviso: solicitud {} rechazada para el negocio '{}': {}",
                request.getId(), request.getBusiness().getName(), request.getRejectionReason());
    }

    @Override
    public void suspended(Business business) {
        log.info("Aviso: negocio '{}' suspendido: {}", business.getName(), business.getSuspensionReason());
    }
}
