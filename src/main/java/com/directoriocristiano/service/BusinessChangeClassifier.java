package com.directoriocristiano.service;

import com.directoriocristiano.dto.BusinessRequest;
import com.directoriocristiano.model.entity.Business;
import com.directoriocristiano.model.entity.ServiceItem;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Separa una edición de un negocio publicado en cambios de publicación inmediata y cambios que
 * requieren aprobación (FR-012, research R4). Vive en el backend para que ningún cliente pueda
 * publicar, por ejemplo, un cambio de teléfono sin revisión.
 */
@Component
public class BusinessChangeClassifier {

    /**
     * @param proposed        estado propuesto de los campos con aprobación, o {@code null} si no hay
     *                        ninguno que haya cambiado
     * @param priceChanges    nuevo precio por id de servicio existente (inmediato)
     * @param removedServices servicios existentes que ya no vienen en la ficha (inmediato)
     */
    public record Result(Map<String, Object> proposed,
                         Map<UUID, String> priceChanges,
                         Set<UUID> removedServices) {

        public boolean needsApproval() {
            return proposed != null;
        }
    }

    public Result classify(Business published, BusinessRequest request) {
        Map<UUID, ServiceItem> existing = new LinkedHashMap<>();
        for (ServiceItem service : published.getServices()) {
            existing.put(service.getId(), service);
        }

        List<BusinessRequest.ServiceRequest> requested =
                request.services() == null ? List.of() : request.services();

        Map<UUID, String> priceChanges = new LinkedHashMap<>();
        Set<UUID> kept = new HashSet<>();
        boolean servicesNeedApproval = false;
        List<Map<String, Object>> proposedServices = new ArrayList<>();

        for (BusinessRequest.ServiceRequest service : requested) {
            ServiceItem current = service.id() == null ? null : existing.get(service.id());
            if (current == null) {
                // Servicio nuevo: requiere aprobación, con su precio.
                servicesNeedApproval = true;
                proposedServices.add(BusinessProposal.service(
                        null, service.name(), service.price(), service.description()));
                continue;
            }
            kept.add(current.getId());
            String newPrice = BusinessProposal.normalize(service.price());
            if (!BusinessProposal.same(newPrice, BusinessProposal.normalize(current.getPrice()))) {
                priceChanges.put(current.getId(), newPrice);
            }
            if (!BusinessProposal.same(BusinessProposal.normalize(service.name()),
                    BusinessProposal.normalize(current.getName()))
                    || !BusinessProposal.same(BusinessProposal.normalize(service.description()),
                    BusinessProposal.normalize(current.getDescription()))) {
                servicesNeedApproval = true;
            }
            // Servicio existente: el precio no viaja en la propuesta (se aplica al instante).
            proposedServices.add(BusinessProposal.service(
                    current.getId().toString(), service.name(), null, service.description()));
        }

        Set<UUID> removed = new HashSet<>(existing.keySet());
        removed.removeAll(kept);

        Map<String, Object> requestedScalars = BusinessProposal.scalarsOf(request);
        boolean scalarsChanged = !requestedScalars.equals(BusinessProposal.scalarsOf(published));

        Map<String, Object> proposed = null;
        if (scalarsChanged || servicesNeedApproval) {
            proposed = new LinkedHashMap<>(requestedScalars);
            proposed.put("services", proposedServices);
        }
        return new Result(proposed, priceChanges, removed);
    }
}
