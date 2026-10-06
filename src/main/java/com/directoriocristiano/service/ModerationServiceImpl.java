package com.directoriocristiano.service;

import com.directoriocristiano.dto.ChangeRequestDetail;
import com.directoriocristiano.dto.ChangeRequestDetail.FieldChange;
import com.directoriocristiano.dto.ModerationEventResponse;
import com.directoriocristiano.dto.ModerationInboxItem;
import com.directoriocristiano.dto.PageResponse;
import com.directoriocristiano.exception.ConflictException;
import com.directoriocristiano.exception.ForbiddenException;
import com.directoriocristiano.exception.ResourceNotFoundException;
import com.directoriocristiano.model.entity.Business;
import com.directoriocristiano.model.entity.BusinessChangeRequest;
import com.directoriocristiano.model.entity.ModerationEvent;
import com.directoriocristiano.model.entity.ServiceItem;
import com.directoriocristiano.model.entity.User;
import com.directoriocristiano.model.enums.BusinessStatus;
import com.directoriocristiano.model.enums.ChangeRequestStatus;
import com.directoriocristiano.model.enums.ChangeRequestType;
import com.directoriocristiano.model.enums.ModerationAction;
import com.directoriocristiano.repository.BusinessChangeRequestRepository;
import com.directoriocristiano.repository.BusinessRepository;
import com.directoriocristiano.repository.ModerationEventRepository;
import com.directoriocristiano.service.notify.ModerationNotifier;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Decisiones de moderación sobre solicitudes de publicación y de edición (specs/002). */
@Service
@RequiredArgsConstructor
public class ModerationServiceImpl implements IModerationService {

    /** Una solicitud que lleva esta cantidad de días o más esperando se destaca (SC-003). */
    static final int OVERDUE_DAYS = 3;

    private static final Map<String, String> LABELS = new LinkedHashMap<>();

    static {
        LABELS.put("name", "Nombre");
        LABELS.put("category", "Rubro");
        LABELS.put("zone", "Zona");
        LABELS.put("description", "Descripción");
        LABELS.put("slogan", "Eslogan");
        LABELS.put("values", "Valores");
        LABELS.put("logoUrl", "Logo");
        LABELS.put("coverUrl", "Foto de portada");
        LABELS.put("contactPhone", "Teléfono");
        LABELS.put("contactWhatsapp", "WhatsApp");
        LABELS.put("contactEmail", "Correo");
        LABELS.put("contactWebsite", "Sitio web");
        LABELS.put("contactAddress", "Dirección");
    }

    private final BusinessChangeRequestRepository changeRequestRepository;
    private final BusinessRepository businessRepository;
    private final ModerationEventRepository moderationEventRepository;
    private final ModerationNotifier notifier;

    /** Bandeja ordenada de la más antigua a la más nueva (FR-016). */
    @Transactional(readOnly = true)
    public PageResponse<ModerationInboxItem> inbox(String status, int page, int size, User moderator) {
        ChangeRequestStatus requested = parseStatus(status);
        Page<BusinessChangeRequest> requests = changeRequestRepository.findByStatusOrderBySubmittedAtAsc(
                requested, PageRequest.of(page, size));
        Instant now = Instant.now();
        List<ModerationInboxItem> content = requests.getContent().stream().map(request -> {
            Business business = request.getBusiness();
            long days = Duration.between(request.getSubmittedAt(), now).toDays();
            return new ModerationInboxItem(
                    request.getId(),
                    request.getType(),
                    business.getId(),
                    business.getName(),
                    business.getCategory(),
                    business.getOwnerName(),
                    request.getSubmittedAt(),
                    days,
                    requested == ChangeRequestStatus.pending && days >= OVERDUE_DAYS,
                    isOwner(business, moderator));
        }).toList();
        return PageResponse.from(requests, content);
    }

    /** Historial de moderación de un negocio, el más reciente primero (FR-024). */
    @Transactional(readOnly = true)
    public List<ModerationEventResponse> history(UUID businessId) {
        if (!businessRepository.existsById(businessId)) {
            throw new ResourceNotFoundException("Negocio", "id", businessId);
        }
        return moderationEventRepository.findByBusinessIdOrderByCreatedAtDesc(businessId).stream()
                .map(ModerationEventResponse::from)
                .toList();
    }

    private static ChangeRequestStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return ChangeRequestStatus.pending;
        }
        try {
            return ChangeRequestStatus.valueOf(status.trim().toLowerCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Indica un estado válido.");
        }
    }

    @Transactional(readOnly = true)
    public ChangeRequestDetail detail(UUID requestId, User moderator) {
        BusinessChangeRequest request = load(requestId);
        return toDetail(request, moderator, changesOf(request));
    }

    @Transactional
    public ChangeRequestDetail approve(UUID requestId, Integer expectedVersion, User moderator) {
        BusinessChangeRequest request = load(requestId);
        checkDecidable(request, expectedVersion, moderator);
        List<FieldChange> changes = changesOf(request);

        Business business = request.getBusiness();
        if (request.getType() == ChangeRequestType.new_business) {
            business.setStatus(BusinessStatus.published);
            business.setPublishedAt(Instant.now());
        } else {
            applyProposal(request.getProposed(), business);
        }
        businessRepository.save(business);

        request.setStatus(ChangeRequestStatus.approved);
        request.setReviewedBy(moderator);
        request.setReviewedAt(Instant.now());
        request = changeRequestRepository.save(request);

        record(business, request, moderator, ModerationAction.approved, null);
        notifier.approved(request);
        return toDetail(request, moderator, changes);
    }

    @Transactional
    public ChangeRequestDetail reject(UUID requestId, Integer expectedVersion, String reason, User moderator) {
        String cleanReason = BusinessProposal.normalize(reason);
        if (cleanReason == null) {
            throw new IllegalArgumentException("Escribe el motivo; el emprendedor lo verá.");
        }
        BusinessChangeRequest request = load(requestId);
        checkDecidable(request, expectedVersion, moderator);
        List<FieldChange> changes = changesOf(request);

        Business business = request.getBusiness();
        if (request.getType() == ChangeRequestType.new_business) {
            business.setStatus(BusinessStatus.draft);
            businessRepository.save(business);
        }

        request.setStatus(ChangeRequestStatus.rejected);
        request.setRejectionReason(cleanReason);
        request.setReviewedBy(moderator);
        request.setReviewedAt(Instant.now());
        request = changeRequestRepository.save(request);

        record(business, request, moderator, ModerationAction.rejected, cleanReason);
        notifier.rejected(request);
        return toDetail(request, moderator, changes);
    }

    private BusinessChangeRequest load(UUID requestId) {
        return changeRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud", "id", requestId));
    }

    /** Reglas comunes a aprobar y rechazar: pendiente, no propia y sin cambios a escondidas. */
    private void checkDecidable(BusinessChangeRequest request, Integer expectedVersion, User moderator) {
        if (request.getStatus() != ChangeRequestStatus.pending) {
            throw new ConflictException("Esta solicitud ya fue resuelta.");
        }
        if (isOwner(request.getBusiness(), moderator)) {
            throw new ForbiddenException("No puedes revisar una solicitud de tu propio negocio.");
        }
        if (expectedVersion == null) {
            throw new IllegalArgumentException("Falta la versión de la solicitud que revisaste.");
        }
        if (expectedVersion != request.getVersion()) {
            throw new ConflictException(
                    "El emprendedor modificó esta solicitud mientras la revisabas. Revísala de nuevo.",
                    toDetail(request, moderator, changesOf(request)));
        }
    }

    /** Copia la propuesta sobre la ficha publicada; los precios existentes no se tocan (FR-012). */
    private void applyProposal(Map<String, Object> proposed, Business business) {
        BusinessProposal.applyScalars(proposed, business);

        Map<String, ServiceItem> existing = new HashMap<>();
        for (ServiceItem service : business.getServices()) {
            existing.put(service.getId().toString(), service);
        }
        List<ServiceItem> ordered = new ArrayList<>();
        for (Map<String, Object> entry : servicesOf(proposed)) {
            Object id = entry.get("id");
            ServiceItem service = id == null ? null : existing.remove(id.toString());
            if (service == null) {
                if (id != null) {
                    continue; // El dueño lo quitó después de proponer el cambio.
                }
                service = ServiceItem.builder()
                        .business(business)
                        .price(text(entry.get("price")))
                        .build();
                business.getServices().add(service);
            }
            service.setName(text(entry.get("name")));
            service.setDescription(text(entry.get("description")));
            ordered.add(service);
        }
        ordered.addAll(existing.values());
        for (int i = 0; i < ordered.size(); i++) {
            ordered.get(i).setSortOrder(i);
        }
    }

    /** Comparación campo por campo entre la versión publicada y la propuesta (FR-017). */
    private List<FieldChange> changesOf(BusinessChangeRequest request) {
        Map<String, Object> after = request.getProposed();
        boolean isNew = request.getType() == ChangeRequestType.new_business;
        Business business = request.getBusiness();
        Map<String, Object> before = isNew ? Map.of() : BusinessProposal.scalarsOf(business);

        List<FieldChange> changes = new ArrayList<>();
        for (Map.Entry<String, String> field : LABELS.entrySet()) {
            String beforeText = display(before.get(field.getKey()));
            String afterText = display(after.get(field.getKey()));
            if (!Objects.equals(beforeText, afterText)) {
                changes.add(new FieldChange(field.getKey(), field.getValue(), beforeText, afterText));
            }
        }

        Map<String, ServiceItem> current = new HashMap<>();
        if (!isNew) {
            for (ServiceItem service : business.getServices()) {
                current.put(service.getId().toString(), service);
            }
        }
        for (Map<String, Object> entry : servicesOf(after)) {
            Object id = entry.get("id");
            ServiceItem existing = id == null ? null : current.get(id.toString());
            if (existing == null) {
                changes.add(new FieldChange("services[new]", isNew ? "Servicio" : "Servicio nuevo", null,
                        serviceText(text(entry.get("name")), text(entry.get("price")), text(entry.get("description")))));
            } else {
                String beforeText = serviceText(existing.getName(), null, existing.getDescription());
                String afterText = serviceText(text(entry.get("name")), null, text(entry.get("description")));
                if (!Objects.equals(beforeText, afterText)) {
                    changes.add(new FieldChange("services[" + id + "]", "Servicio", beforeText, afterText));
                }
            }
        }
        return changes;
    }

    private ChangeRequestDetail toDetail(BusinessChangeRequest request, User viewer, List<FieldChange> changes) {
        Business business = request.getBusiness();
        User submitter = request.getSubmittedBy();
        return new ChangeRequestDetail(
                request.getId(),
                request.getType(),
                request.getStatus(),
                request.getVersion(),
                new ChangeRequestDetail.BusinessRef(business.getId(), business.getName(), business.getStatus()),
                new ChangeRequestDetail.UserRef(submitter.getId(), submitter.getDisplayName()),
                request.getSubmittedAt(),
                isOwner(business, viewer),
                request.getRejectionReason(),
                request.getReviewedAt(),
                changes);
    }

    private void record(Business business, BusinessChangeRequest request, User actor,
                        ModerationAction action, String reason) {
        moderationEventRepository.save(ModerationEvent.builder()
                .business(business)
                .changeRequest(request)
                .actor(actor)
                .action(action)
                .reason(reason)
                .build());
    }

    private static boolean isOwner(Business business, User user) {
        return user != null && business.getOwner() != null
                && business.getOwner().getId().equals(user.getId());
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> servicesOf(Map<String, Object> proposed) {
        Object services = proposed.get("services");
        if (!(services instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(Map.class::isInstance)
                .map(item -> (Map<String, Object>) item)
                .toList();
    }

    private static String display(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof List<?> list) {
            return list.isEmpty() ? null : String.join(", ", list.stream().map(String::valueOf).toList());
        }
        return BusinessProposal.normalize(value.toString());
    }

    private static String text(Object value) {
        return value == null ? null : BusinessProposal.normalize(value.toString());
    }

    private static String serviceText(String name, String price, String description) {
        StringBuilder text = new StringBuilder(name == null ? "" : name);
        if (price != null) {
            text.append(" — ").append(price);
        }
        if (description != null) {
            text.append(" (").append(description).append(")");
        }
        return text.toString();
    }
}
