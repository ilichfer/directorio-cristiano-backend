package com.directoriocristiano.service;

import com.directoriocristiano.dto.BusinessRequest;
import com.directoriocristiano.dto.BusinessRequest.ServiceRequest;
import com.directoriocristiano.dto.ChangeRequestDetail;
import com.directoriocristiano.dto.OwnerBusinessResponse;
import com.directoriocristiano.exception.ConflictException;
import com.directoriocristiano.exception.ForbiddenException;
import com.directoriocristiano.exception.IncompleteBusinessException;
import com.directoriocristiano.model.entity.Business;
import com.directoriocristiano.model.entity.BusinessChangeRequest;
import com.directoriocristiano.model.entity.ServiceItem;
import com.directoriocristiano.model.entity.User;
import com.directoriocristiano.model.enums.BusinessStatus;
import com.directoriocristiano.model.enums.ChangeRequestStatus;
import com.directoriocristiano.model.enums.ChangeRequestType;
import com.directoriocristiano.model.enums.UserType;
import com.directoriocristiano.repository.BusinessChangeRequestRepository;
import com.directoriocristiano.repository.BusinessRepository;
import com.directoriocristiano.repository.ModerationEventRepository;
import com.directoriocristiano.repository.ReviewRepository;
import com.directoriocristiano.service.notify.ModerationNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Ciclo de moderación de punta a punta sobre los servicios, con los repositorios simulados en
 * memoria: negocio nuevo (US2) y edición de uno publicado (US3).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ModerationServiceTest {

    @Mock
    private BusinessRepository businessRepository;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private BusinessChangeRequestRepository changeRequestRepository;
    @Mock
    private ModerationEventRepository moderationEventRepository;
    @Mock
    private ModerationNotifier notifier;

    private BusinessServiceImpl businessService;
    private ModerationServiceImpl moderationService;

    private final Map<UUID, Business> businesses = new HashMap<>();
    private final Map<UUID, BusinessChangeRequest> requests = new HashMap<>();

    private User owner;
    private User moderator;

    @BeforeEach
    void setUp() {
        businessService = new BusinessServiceImpl(businessRepository, reviewRepository,
                changeRequestRepository, moderationEventRepository, new BusinessChangeClassifier());
        moderationService = new ModerationServiceImpl(changeRequestRepository, businessRepository,
                moderationEventRepository, notifier);

        owner = User.builder().id(UUID.randomUUID()).email("duena@ejemplo.com").displayName("Dueña")
                .userType(UserType.entrepreneur).build();
        moderator = User.builder().id(UUID.randomUUID()).email("mod@ejemplo.com").displayName("Moderador")
                .userType(UserType.buyer).moderator(true).build();

        // Repositorios en memoria
        when(businessRepository.save(any(Business.class))).thenAnswer(inv -> {
            Business b = inv.getArgument(0);
            if (b.getId() == null) {
                b.setId(UUID.randomUUID());
            }
            for (ServiceItem s : b.getServices()) {
                if (s.getId() == null) {
                    s.setId(UUID.randomUUID());
                }
            }
            businesses.put(b.getId(), b);
            return b;
        });
        when(businessRepository.findById(any())).thenAnswer(inv -> Optional.ofNullable(businesses.get(inv.<UUID>getArgument(0))));
        when(changeRequestRepository.save(any(BusinessChangeRequest.class))).thenAnswer(inv -> {
            BusinessChangeRequest r = inv.getArgument(0);
            if (r.getId() == null) {
                r.setId(UUID.randomUUID());
            }
            requests.put(r.getId(), r);
            return r;
        });
        when(changeRequestRepository.findById(any())).thenAnswer(inv -> Optional.ofNullable(requests.get(inv.<UUID>getArgument(0))));
        when(changeRequestRepository.findByBusinessIdAndStatus(any(), any())).thenAnswer(inv -> requests.values().stream()
                .filter(r -> r.getBusiness().getId().equals(inv.getArgument(0)) && r.getStatus() == inv.getArgument(1))
                .findFirst());
        when(changeRequestRepository.findFirstByBusinessIdAndReviewedAtNotNullOrderByReviewedAtDesc(any()))
                .thenReturn(Optional.empty());
    }

    @Nested
    class NegocioNuevo {

        @Test
        void seCreaComoBorrador() {
            OwnerBusinessResponse created = businessService.create(complete(), owner);
            assertThat(created.business().status()).isEqualTo(BusinessStatus.draft);
        }

        @Test
        void unClienteSinPerfilDeEmprendedorNoPuedeCrear() {
            owner.setUserType(UserType.buyer);
            assertThatThrownBy(() -> businessService.create(complete(), owner))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        void enviarIncompletoListaLoQueFalta() {
            var draft = businessService.create(new BusinessRequest("Solo nombre", null, null, null, null, null,
                    null, null, null, null, null, null, null, null), owner);

            assertThatThrownBy(() -> businessService.submit(draft.business().id(), owner))
                    .isInstanceOfSatisfying(IncompleteBusinessException.class, e -> assertThat(e.getMissing())
                            .contains("Rubro", "Zona", "Descripción")
                            .anyMatch(m -> m.startsWith("Al menos un dato de contacto")));
        }

        @Test
        void enviarCompletoCreaUnaPendienteYPasaARevision() {
            UUID id = businessService.create(complete(), owner).business().id();

            OwnerBusinessResponse submitted = businessService.submit(id, owner);

            assertThat(submitted.business().status()).isEqualTo(BusinessStatus.in_review);
            assertThat(submitted.pendingRequest()).isNotNull();
            assertThat(submitted.pendingRequest().type()).isEqualTo(ChangeRequestType.new_business);
            assertThatThrownBy(() -> businessService.submit(id, owner)).isInstanceOf(ConflictException.class);
        }

        @Test
        void aprobarLoPublica() {
            BusinessChangeRequest request = submittedNew();

            moderationService.approve(request.getId(), 0, moderator);

            Business business = request.getBusiness();
            assertThat(business.getStatus()).isEqualTo(BusinessStatus.published);
            assertThat(business.getPublishedAt()).isNotNull();
            assertThat(request.getStatus()).isEqualTo(ChangeRequestStatus.approved);
            verify(notifier).approved(request);
        }

        @Test
        void rechazarSinMotivoNoSePermite() {
            BusinessChangeRequest request = submittedNew();
            assertThatThrownBy(() -> moderationService.reject(request.getId(), 0, "  ", moderator))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("motivo");
        }

        @Test
        void rechazarConMotivoVuelveABorrador() {
            BusinessChangeRequest request = submittedNew();

            moderationService.reject(request.getId(), 0, "La foto no corresponde al negocio.", moderator);

            assertThat(request.getBusiness().getStatus()).isEqualTo(BusinessStatus.draft);
            assertThat(request.getRejectionReason()).isEqualTo("La foto no corresponde al negocio.");
            verify(notifier).rejected(request);
        }

        @Test
        void versionDistintaDaConflictoConElDetalleActual() {
            BusinessChangeRequest request = submittedNew();
            assertThatThrownBy(() -> moderationService.approve(request.getId(), 7, moderator))
                    .isInstanceOfSatisfying(ConflictException.class,
                            e -> assertThat(e.getCurrent()).isInstanceOf(ChangeRequestDetail.class));
        }

        @Test
        void aprobarUnaYaResueltaDaConflicto() {
            BusinessChangeRequest request = submittedNew();
            moderationService.approve(request.getId(), 0, moderator);
            assertThatThrownBy(() -> moderationService.approve(request.getId(), 0, moderator))
                    .isInstanceOf(ConflictException.class);
        }

        @Test
        void unModeradorNoRevisaSuPropioNegocio() {
            owner.setModerator(true);
            BusinessChangeRequest request = submittedNew();
            assertThatThrownBy(() -> moderationService.approve(request.getId(), 0, owner))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        void cancelarElEnvioVuelveABorrador() {
            BusinessChangeRequest request = submittedNew();
            businessService.cancelChangeRequest(request.getBusiness().getId(), owner);
            assertThat(request.getStatus()).isEqualTo(ChangeRequestStatus.cancelled);
            assertThat(request.getBusiness().getStatus()).isEqualTo(BusinessStatus.draft);
        }
    }

    @Nested
    class EdicionDeUnPublicado {

        private Business business;
        private ServiceItem clase;

        @BeforeEach
        void publish() {
            clase = ServiceItem.builder().id(UUID.randomUUID()).name("Clase particular").price("$12.000").build();
            business = Business.builder().id(UUID.randomUUID()).owner(owner).ownerName("Dueña")
                    .name("Tutorías").category("Educación y Mentoría").zone("Zona Este").description("Clases")
                    .contactPhone("+56 9 1111 2222").rating(BigDecimal.ZERO)
                    .status(BusinessStatus.published)
                    .values(new ArrayList<>()).services(new ArrayList<>(List.of(clase))).reviews(new ArrayList<>())
                    .build();
            clase.setBusiness(business);
            businesses.put(business.getId(), business);
        }

        @Test
        void cambioDeTelefonoNoTocaLaVersionPublicada() {
            OwnerBusinessResponse response = businessService.update(business.getId(), edit("+56 9 7000 1111", "$12.000"), owner);

            assertThat(business.getContactPhone()).isEqualTo("+56 9 1111 2222");
            assertThat(business.getStatus()).isEqualTo(BusinessStatus.published);
            assertThat(response.pendingRequest()).isNotNull();
            assertThat(response.pendingRequest().proposed()).containsEntry("contactPhone", "+56 9 7000 1111");
        }

        @Test
        void elPrecioSePublicaAlInstante() {
            OwnerBusinessResponse response = businessService.update(business.getId(), edit("+56 9 1111 2222", "$15.000"), owner);

            assertThat(clase.getPrice()).isEqualTo("$15.000");
            assertThat(response.pendingRequest()).isNull();
            assertThat(response.appliedImmediately()).containsExactly("Precio de «Clase particular»");
        }

        @Test
        void dosEdicionesDejanUnaSolaPendiente() {
            businessService.update(business.getId(), edit("+56 9 7000 1111", "$12.000"), owner);
            businessService.update(business.getId(), edit("+56 9 7000 2222", "$12.000"), owner);

            assertThat(requests.values()).filteredOn(r -> r.getStatus() == ChangeRequestStatus.pending).hasSize(1);
            assertThat(requests.values().iterator().next().getProposed()).containsEntry("contactPhone", "+56 9 7000 2222");
        }

        @Test
        void aprobarAplicaLaPropuesta() {
            businessService.update(business.getId(), edit("+56 9 7000 1111", "$12.000"), owner);
            BusinessChangeRequest pending = requests.values().iterator().next();

            ChangeRequestDetail detail = moderationService.approve(pending.getId(), 0, moderator);

            assertThat(detail.changes()).extracting(ChangeRequestDetail.FieldChange::field).containsExactly("contactPhone");
            assertThat(business.getContactPhone()).isEqualTo("+56 9 7000 1111");
            assertThat(clase.getPrice()).isEqualTo("$12.000");
        }

        @Test
        void cancelarDejaLaFichaIgual() {
            businessService.update(business.getId(), edit("+56 9 7000 1111", "$12.000"), owner);
            businessService.cancelChangeRequest(business.getId(), owner);

            assertThat(business.getContactPhone()).isEqualTo("+56 9 1111 2222");
            assertThat(business.getStatus()).isEqualTo(BusinessStatus.published);
        }

        @Test
        void unSuspendidoNoSePuedeEditar() {
            business.setStatus(BusinessStatus.suspended);
            assertThatThrownBy(() -> businessService.update(business.getId(), edit("+56 9 7000 1111", "$12.000"), owner))
                    .isInstanceOf(ConflictException.class);
        }

        @Test
        void pausarYReanudarSinRevision() {
            assertThat(businessService.pause(business.getId(), owner).business().status()).isEqualTo(BusinessStatus.paused);
            assertThat(businessService.resume(business.getId(), owner).business().status()).isEqualTo(BusinessStatus.published);
            assertThat(requests).isEmpty();
        }

        private BusinessRequest edit(String phone, String price) {
            return new BusinessRequest("Tutorías", "Educación y Mentoría", "Zona Este", "Clases", null, null, null,
                    List.of(), phone, null, null, null, null,
                    List.of(new ServiceRequest(clase.getId(), "Clase particular", price, null)));
        }
    }

    private BusinessChangeRequest submittedNew() {
        UUID id = businessService.create(complete(), owner).business().id();
        businessService.submit(id, owner);
        return requests.values().stream()
                .filter(r -> r.getBusiness().getId().equals(id))
                .findFirst().orElseThrow();
    }

    private static BusinessRequest complete() {
        return new BusinessRequest("Pan de la Casa", "Alimentos y Repostería", "Zona Centro",
                "Pan amasado todos los días.", null, null, null, List.of("Honestidad"),
                "+56 9 3333 4444", null, null, null, null,
                List.of(new ServiceRequest(null, "Pan amasado (docena)", "$3.000", null)));
    }
}
