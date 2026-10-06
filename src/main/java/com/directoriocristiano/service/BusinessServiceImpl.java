package com.directoriocristiano.service;

import com.directoriocristiano.dto.*;
import com.directoriocristiano.exception.ConflictException;
import com.directoriocristiano.exception.ForbiddenException;
import com.directoriocristiano.exception.IncompleteBusinessException;
import com.directoriocristiano.exception.ResourceNotFoundException;
import com.directoriocristiano.model.entity.Business;
import com.directoriocristiano.model.entity.BusinessChangeRequest;
import com.directoriocristiano.model.entity.ModerationEvent;
import com.directoriocristiano.model.entity.Review;
import com.directoriocristiano.model.entity.ServiceItem;
import com.directoriocristiano.model.entity.User;
import com.directoriocristiano.model.enums.BusinessStatus;
import com.directoriocristiano.model.enums.ChangeRequestStatus;
import com.directoriocristiano.model.enums.ChangeRequestType;
import com.directoriocristiano.model.enums.ModerationAction;
import com.directoriocristiano.model.enums.UserType;
import com.directoriocristiano.repository.BusinessChangeRequestRepository;
import com.directoriocristiano.repository.BusinessRepository;
import com.directoriocristiano.repository.ModerationEventRepository;
import com.directoriocristiano.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BusinessServiceImpl implements IBusinessService {

    private final BusinessRepository businessRepository;
    private final ReviewRepository reviewRepository;
    private final BusinessChangeRequestRepository changeRequestRepository;
    private final ModerationEventRepository moderationEventRepository;
    private final BusinessChangeClassifier changeClassifier;

    // ---------------------------------------------------------------- Lecturas públicas

    public PageResponse<BusinessResponse> getAll(String search, String category, String zone,
                                                  int page, int size, String sortBy, String sortDir,
                                                  User viewer) {
        Pageable pageable = PageRequest.of(page, size, Sort.unsorted());

        String searchParam = (search != null && !search.isBlank()) ? search : null;
        String categoryParam = (category != null && !category.isBlank() && !category.equals("Todos")) ? category : null;
        String zoneParam = (zone != null && !zone.isBlank() && !zone.equals("Todas")) ? zone : null;

        Page<Business> businessPage = businessRepository.searchBusinesses(
                searchParam, categoryParam, zoneParam, pageable);

        List<BusinessResponse> content = businessPage.getContent().stream()
                .map(b -> BusinessResponse.from(b, viewer != null))
                .toList();

        return PageResponse.from(businessPage, content);
    }

    public BusinessResponse getById(UUID id, User viewer) {
        Business business = findVisible(id, viewer);
        return BusinessResponse.from(business, viewer != null);
    }

    /**
     * Un negocio no publicado no existe para el público (FR-007): solo lo ven su dueño y los
     * moderadores.
     */
    private Business findVisible(UUID id, User viewer) {
        Business business = businessRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Negocio", "id", id));
        if (business.getStatus() != BusinessStatus.published && !isOwnerOrModerator(business, viewer)) {
            throw new ResourceNotFoundException("Negocio", "id", id);
        }
        return business;
    }

    private boolean isOwnerOrModerator(Business business, User viewer) {
        if (viewer == null) {
            return false;
        }
        return viewer.isModerator() || isOwner(business, viewer);
    }

    private boolean isOwner(Business business, User user) {
        return user != null && business.getOwner() != null
                && business.getOwner().getId().equals(user.getId());
    }

    // ---------------------------------------------------------------- Gestión del dueño

    /** Todos los negocios del dueño, en cualquier estado (FR-015). */
    @Transactional(readOnly = true)
    public List<OwnerBusinessResponse> getMyBusinesses(User user) {
        return businessRepository.findByOwnerId(user.getId()).stream()
                .map(b -> toOwnerResponse(b, List.of()))
                .toList();
    }

    /** Todo negocio nace como borrador; nada se publica sin aprobación (FR-009). */
    @Transactional
    public OwnerBusinessResponse create(BusinessRequest request, User owner) {
        requireEntrepreneur(owner);
        Business business = Business.builder()
                .owner(owner)
                .ownerName(owner.getDisplayName())
                .rating(BigDecimal.ZERO)
                .featured(false)
                .status(BusinessStatus.draft)
                .values(new ArrayList<>())
                .services(new ArrayList<>())
                .reviews(new ArrayList<>())
                .build();
        BusinessProposal.applyScalars(BusinessProposal.scalarsOf(request), business);
        syncServices(business, request.services());

        business = businessRepository.save(business);
        return toOwnerResponse(business, List.of());
    }

    /**
     * Guardar según el estado (contracts/owner-businesses.md): un borrador se sobrescribe; uno en
     * revisión actualiza también su solicitud pendiente; uno publicado o pausado aplica al instante
     * solo los cambios de bajo riesgo y deja el resto en una solicitud pendiente, sin tocar la
     * versión publicada (FR-010).
     */
    @Transactional
    public OwnerBusinessResponse update(UUID id, BusinessRequest request, User owner) {
        requireEntrepreneur(owner);
        Business business = findOwned(id, owner);

        return switch (business.getStatus()) {
            case draft -> {
                overwrite(business, request);
                yield toOwnerResponse(businessRepository.save(business), List.of());
            }
            case in_review -> {
                overwrite(business, request);
                business = businessRepository.save(business);
                BusinessChangeRequest pending = requirePending(business);
                pending.setProposed(BusinessProposal.snapshot(business));
                pending.setSubmittedAt(Instant.now());
                changeRequestRepository.save(pending);
                record(business, pending, owner, ModerationAction.resubmitted, null);
                yield toOwnerResponse(business, List.of());
            }
            case published, paused -> updatePublished(business, request, owner);
            case suspended -> throw new ConflictException("Este negocio está suspendido; no se puede editar.");
        };
    }

    private OwnerBusinessResponse updatePublished(Business business, BusinessRequest request, User owner) {
        BusinessChangeClassifier.Result result = changeClassifier.classify(business, request);
        List<String> applied = applyImmediate(business, result);
        business = businessRepository.save(business);

        if (result.needsApproval()) {
            BusinessChangeRequest pending = changeRequestRepository
                    .findByBusinessIdAndStatus(business.getId(), ChangeRequestStatus.pending)
                    .orElse(null);
            if (pending == null) {
                pending = BusinessChangeRequest.builder()
                        .business(business)
                        .type(ChangeRequestType.update)
                        .status(ChangeRequestStatus.pending)
                        .submittedBy(owner)
                        .build();
                pending.setProposed(result.proposed());
                pending.setSubmittedAt(Instant.now());
                pending = changeRequestRepository.save(pending);
                record(business, pending, owner, ModerationAction.submitted, null);
            } else {
                // Una nueva edición antes de la revisión actualiza la misma solicitud (FR-011).
                pending.setProposed(result.proposed());
                pending.setSubmittedAt(Instant.now());
                pending = changeRequestRepository.save(pending);
                record(business, pending, owner, ModerationAction.resubmitted, null);
            }
        }
        return toOwnerResponse(business, applied);
    }

    /** Precios de servicios existentes y servicios quitados: se publican al instante (FR-012). */
    private List<String> applyImmediate(Business business, BusinessChangeClassifier.Result result) {
        List<String> applied = new ArrayList<>();
        List<ServiceItem> toRemove = new ArrayList<>();
        for (ServiceItem service : business.getServices()) {
            if (result.removedServices().contains(service.getId())) {
                toRemove.add(service);
                applied.add("Se quitó el servicio «%s»".formatted(service.getName()));
            } else if (result.priceChanges().containsKey(service.getId())) {
                service.setPrice(result.priceChanges().get(service.getId()));
                applied.add("Precio de «%s»".formatted(service.getName()));
            }
        }
        business.getServices().removeAll(toRemove);
        return applied;
    }

    /** Envía un borrador a revisión (FR-008, FR-009). */
    @Transactional
    public OwnerBusinessResponse submit(UUID id, User owner) {
        requireEntrepreneur(owner);
        Business business = findOwned(id, owner);
        if (business.getStatus() != BusinessStatus.draft) {
            throw new ConflictException("Este negocio ya fue enviado a revisión.");
        }

        List<String> missing = new ArrayList<>();
        if (BusinessProposal.normalize(business.getName()) == null) missing.add("Nombre");
        if (BusinessProposal.normalize(business.getCategory()) == null) missing.add("Rubro");
        if (BusinessProposal.normalize(business.getZone()) == null) missing.add("Zona");
        if (BusinessProposal.normalize(business.getDescription()) == null) missing.add("Descripción");
        if (BusinessProposal.normalize(business.getContactPhone()) == null
                && BusinessProposal.normalize(business.getContactWhatsapp()) == null
                && BusinessProposal.normalize(business.getContactEmail()) == null) {
            missing.add("Al menos un dato de contacto (teléfono, WhatsApp o correo)");
        }
        if (!missing.isEmpty()) {
            throw new IncompleteBusinessException(missing);
        }

        BusinessChangeRequest request = BusinessChangeRequest.builder()
                .business(business)
                .type(ChangeRequestType.new_business)
                .status(ChangeRequestStatus.pending)
                .submittedBy(owner)
                .build();
        request.setProposed(BusinessProposal.snapshot(business));
        request.setSubmittedAt(Instant.now());
        request = changeRequestRepository.save(request);

        business.setStatus(BusinessStatus.in_review);
        business = businessRepository.save(business);
        record(business, request, owner, ModerationAction.submitted, null);
        return toOwnerResponse(business, List.of());
    }

    /** Cancela la solicitud pendiente (FR-013). Un negocio nuevo vuelve a borrador. */
    @Transactional
    public OwnerBusinessResponse cancelChangeRequest(UUID id, User owner) {
        Business business = findOwned(id, owner);
        BusinessChangeRequest pending = requirePending(business);
        pending.setStatus(ChangeRequestStatus.cancelled);
        changeRequestRepository.save(pending);
        if (pending.getType() == ChangeRequestType.new_business) {
            business.setStatus(BusinessStatus.draft);
            business = businessRepository.save(business);
        }
        record(business, pending, owner, ModerationAction.cancelled, null);
        return toOwnerResponse(business, List.of());
    }

    /** Pausar y reanudar no pasan por revisión (FR-014). */
    @Transactional
    public OwnerBusinessResponse pause(UUID id, User owner) {
        Business business = findOwned(id, owner);
        if (business.getStatus() != BusinessStatus.published) {
            throw new ConflictException("Solo se puede pausar un negocio publicado.");
        }
        business.setStatus(BusinessStatus.paused);
        business = businessRepository.save(business);
        record(business, null, owner, ModerationAction.paused, null);
        return toOwnerResponse(business, List.of());
    }

    @Transactional
    public OwnerBusinessResponse resume(UUID id, User owner) {
        Business business = findOwned(id, owner);
        if (business.getStatus() != BusinessStatus.paused) {
            throw new ConflictException("Solo se puede reanudar un negocio pausado.");
        }
        business.setStatus(BusinessStatus.published);
        business = businessRepository.save(business);
        record(business, null, owner, ModerationAction.resumed, null);
        return toOwnerResponse(business, List.of());
    }

    @Transactional
    public void delete(UUID id, User owner) {
        Business business = businessRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Negocio", "id", id));

        if (!isOwner(business, owner)) {
            throw new ForbiddenException("No tienes permiso para eliminar este negocio.");
        }

        businessRepository.delete(business);
    }

    private void requireEntrepreneur(User user) {
        if (user == null || user.getUserType() != UserType.entrepreneur) {
            throw new ForbiddenException("Activa tu perfil de emprendedor para gestionar negocios.");
        }
    }

    private Business findOwned(UUID id, User owner) {
        Business business = businessRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Negocio", "id", id));
        if (!isOwner(business, owner)) {
            throw new ForbiddenException("No tienes permiso para modificar este negocio.");
        }
        return business;
    }

    private BusinessChangeRequest requirePending(Business business) {
        return changeRequestRepository
                .findByBusinessIdAndStatus(business.getId(), ChangeRequestStatus.pending)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud pendiente", "negocio", business.getId()));
    }

    private void overwrite(Business business, BusinessRequest request) {
        BusinessProposal.applyScalars(BusinessProposal.scalarsOf(request), business);
        syncServices(business, request.services());
    }

    /**
     * Actualiza los servicios por id en vez de borrarlos y recrearlos (research R4): los que no
     * vienen se quitan, los que traen id se actualizan y los que no traen id se crean.
     */
    private void syncServices(Business business, List<BusinessRequest.ServiceRequest> requested) {
        List<BusinessRequest.ServiceRequest> incoming = requested == null ? List.of() : requested;
        Map<UUID, ServiceItem> existing = new HashMap<>();
        for (ServiceItem service : business.getServices()) {
            existing.put(service.getId(), service);
        }

        List<ServiceItem> result = new ArrayList<>();
        for (int i = 0; i < incoming.size(); i++) {
            BusinessRequest.ServiceRequest req = incoming.get(i);
            ServiceItem service = req.id() == null ? null : existing.get(req.id());
            if (service == null) {
                service = ServiceItem.builder().business(business).build();
            }
            service.setName(BusinessProposal.normalize(req.name()));
            service.setPrice(BusinessProposal.normalize(req.price()));
            service.setDescription(BusinessProposal.normalize(req.description()));
            service.setSortOrder(i);
            result.add(service);
        }
        business.getServices().retainAll(result);
        for (ServiceItem service : result) {
            if (!business.getServices().contains(service)) {
                business.getServices().add(service);
            }
        }
    }

    OwnerBusinessResponse toOwnerResponse(Business business, List<String> appliedImmediately) {
        BusinessChangeRequest pending = changeRequestRepository
                .findByBusinessIdAndStatus(business.getId(), ChangeRequestStatus.pending)
                .orElse(null);
        // Solo se muestra el último rechazo si fue la última decisión y no hay otra solicitud en curso.
        BusinessChangeRequest lastRejected = pending != null ? null : changeRequestRepository
                .findFirstByBusinessIdAndReviewedAtNotNullOrderByReviewedAtDesc(business.getId())
                .filter(r -> r.getStatus() == ChangeRequestStatus.rejected)
                .orElse(null);
        return OwnerBusinessResponse.from(business, pending, lastRejected, appliedImmediately);
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

    // ---------------------------------------------------------------- Reseñas

    @Transactional
    public ReviewResponse addReview(UUID businessId, ReviewRequest request, User user) {
        Business business = businessRepository.findById(businessId)
                .filter(b -> b.getStatus() == BusinessStatus.published)
                .orElseThrow(() -> new ResourceNotFoundException("Negocio", "id", businessId));

        if (isOwner(business, user)) {
            throw new ForbiddenException("No puedes dejar un testimonio en tu propio negocio.");
        }

        Review review = Review.builder()
                .business(business)
                .user(user)
                .authorName(request.authorName())
                .rating(request.rating())
                .comment(request.comment())
                .honesty(request.honesty())
                .quality(request.quality())
                .punctuality(request.punctuality())
                .kindness(request.kindness())
                .verifiedClient(user != null)
                .build();

        review = reviewRepository.save(review);
        updateBusinessRating(business);

        return ReviewResponse.from(review);
    }

    public PageResponse<ReviewResponse> getReviews(UUID businessId, int page, int size, User viewer) {
        findVisible(businessId, viewer);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Review> reviewPage = reviewRepository.findByBusinessIdOrderByCreatedAtDesc(businessId, pageable);

        List<ReviewResponse> content = reviewPage.getContent().stream()
                .map(ReviewResponse::from)
                .toList();

        return PageResponse.from(reviewPage, content);
    }

    private void updateBusinessRating(Business business) {
        Double avg = reviewRepository.averageRatingByBusinessId(business.getId());
        business.setRating(BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP));
        businessRepository.save(business);
    }
}
