package com.directoriocristiano.service;

import com.directoriocristiano.dto.UserProfileResponse;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private BusinessRepository businessRepository;
    @Mock
    private BusinessChangeRequestRepository changeRequestRepository;
    @Mock
    private ModerationEventRepository moderationEventRepository;

    @InjectMocks
    private ProfileServiceImpl profileService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).email("ana@ejemplo.com").displayName("Ana")
                .userType(UserType.buyer).build();
        lenient().when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        lenient().when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void activarSinAceptarElAcuerdoFalla() {
        assertThatThrownBy(() -> profileService.activateEntrepreneur(user, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("acuerdo de honestidad");
        verify(userRepository, never()).save(any());
    }

    @Test
    void activarConvierteAEmprendedorYRegistraLaFecha() {
        UserProfileResponse profile = profileService.activateEntrepreneur(user, true);

        assertThat(profile.isEntrepreneur()).isTrue();
        assertThat(profile.userType()).isEqualTo(UserType.entrepreneur);
        assertThat(profile.entrepreneurAgreementAt()).isNotNull();
    }

    @Test
    void activarEsIdempotente() {
        Instant original = Instant.parse("2026-01-01T00:00:00Z");
        user.setUserType(UserType.entrepreneur);
        user.setEntrepreneurAgreementAt(original);

        UserProfileResponse profile = profileService.activateEntrepreneur(user, true);

        assertThat(profile.entrepreneurAgreementAt()).isEqualTo(original);
        verify(userRepository, never()).save(any());
    }

    @Test
    void desactivarPausaPublicadosYCancelaPendientes() {
        user.setUserType(UserType.entrepreneur);
        Business published = Business.builder().id(UUID.randomUUID()).owner(user)
                .status(BusinessStatus.published).build();
        Business inReview = Business.builder().id(UUID.randomUUID()).owner(user)
                .status(BusinessStatus.in_review).build();
        BusinessChangeRequest pendingNew = BusinessChangeRequest.builder().id(UUID.randomUUID())
                .business(inReview).type(ChangeRequestType.new_business)
                .status(ChangeRequestStatus.pending).build();

        when(businessRepository.findByOwnerId(user.getId())).thenReturn(List.of(published, inReview));
        when(changeRequestRepository.findByBusinessIdAndStatus(published.getId(), ChangeRequestStatus.pending))
                .thenReturn(Optional.empty());
        when(changeRequestRepository.findByBusinessIdAndStatus(inReview.getId(), ChangeRequestStatus.pending))
                .thenReturn(Optional.of(pendingNew));

        UserProfileResponse profile = profileService.deactivateEntrepreneur(user);

        assertThat(profile.isEntrepreneur()).isFalse();
        assertThat(published.getStatus()).isEqualTo(BusinessStatus.paused);
        assertThat(inReview.getStatus()).isEqualTo(BusinessStatus.draft);
        assertThat(pendingNew.getStatus()).isEqualTo(ChangeRequestStatus.cancelled);

        ArgumentCaptor<ModerationEvent> events = ArgumentCaptor.forClass(ModerationEvent.class);
        verify(moderationEventRepository, times(2)).save(events.capture());
        assertThat(events.getAllValues()).extracting(ModerationEvent::getAction)
                .containsExactlyInAnyOrder(ModerationAction.paused, ModerationAction.cancelled);
    }
}
