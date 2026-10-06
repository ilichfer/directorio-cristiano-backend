package com.directoriocristiano.service;

import com.directoriocristiano.dto.UserProfileResponse;
import com.directoriocristiano.exception.ResourceNotFoundException;
import com.directoriocristiano.model.entity.Business;
import com.directoriocristiano.model.entity.BusinessChangeRequest;
import com.directoriocristiano.model.entity.ModerationEvent;
import com.directoriocristiano.model.entity.User;
import com.directoriocristiano.model.enums.BusinessStatus;
import com.directoriocristiano.model.enums.ChangeRequestStatus;
import com.directoriocristiano.model.enums.ChangeRequestType;
import com.directoriocristiano.model.enums.ModerationAction;
import com.directoriocristiano.model.enums.UserType;
import com.directoriocristiano.repository.BusinessChangeRequestRepository;
import com.directoriocristiano.repository.BusinessRepository;
import com.directoriocristiano.repository.ModerationEventRepository;
import com.directoriocristiano.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Perfiles de una misma cuenta: siempre cliente, y emprendedor si acepta el acuerdo de honestidad
 * (specs/002, US1).
 */
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements IProfileService {

    private final UserRepository userRepository;
    private final BusinessRepository businessRepository;
    private final BusinessChangeRequestRepository changeRequestRepository;
    private final ModerationEventRepository moderationEventRepository;

    @Transactional
    public UserProfileResponse activateEntrepreneur(User principal, boolean acceptAgreement) {
        if (!acceptAgreement) {
            throw new IllegalArgumentException(
                    "Debes aceptar el acuerdo de honestidad y trato justo para publicar un negocio.");
        }
        User user = reload(principal);
        if (user.getUserType() != UserType.entrepreneur) {
            user.setUserType(UserType.entrepreneur);
            user.setEntrepreneurAgreementAt(Instant.now());
            user = userRepository.save(user);
        }
        return UserProfileResponse.from(user);
    }

    /**
     * La cuenta sigue como cliente. Sus negocios publicados se pausan y sus solicitudes pendientes
     * se cancelan, para que nada quede publicado ni en revisión sin un emprendedor detrás.
     */
    @Transactional
    public UserProfileResponse deactivateEntrepreneur(User principal) {
        User user = reload(principal);
        if (user.getUserType() != UserType.entrepreneur) {
            return UserProfileResponse.from(user);
        }

        for (Business business : businessRepository.findByOwnerId(user.getId())) {
            changeRequestRepository.findByBusinessIdAndStatus(business.getId(), ChangeRequestStatus.pending)
                    .ifPresent(request -> cancel(request, user));
            if (business.getStatus() == BusinessStatus.published) {
                business.setStatus(BusinessStatus.paused);
                businessRepository.save(business);
                record(business, null, user, ModerationAction.paused);
            }
        }

        user.setUserType(UserType.buyer);
        return UserProfileResponse.from(userRepository.save(user));
    }

    private void cancel(BusinessChangeRequest request, User actor) {
        request.setStatus(ChangeRequestStatus.cancelled);
        changeRequestRepository.save(request);
        Business business = request.getBusiness();
        if (request.getType() == ChangeRequestType.new_business) {
            business.setStatus(BusinessStatus.draft);
            businessRepository.save(business);
        }
        record(business, request, actor, ModerationAction.cancelled);
    }

    private void record(Business business, BusinessChangeRequest request, User actor, ModerationAction action) {
        moderationEventRepository.save(ModerationEvent.builder()
                .business(business)
                .changeRequest(request)
                .actor(actor)
                .action(action)
                .build());
    }

    private User reload(User principal) {
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", principal.getId()));
    }
}
