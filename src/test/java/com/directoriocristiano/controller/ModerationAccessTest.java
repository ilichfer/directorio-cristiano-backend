package com.directoriocristiano.controller;

import com.directoriocristiano.config.SecurityConfig;
import com.directoriocristiano.model.entity.Business;
import com.directoriocristiano.model.entity.BusinessChangeRequest;
import com.directoriocristiano.model.entity.User;
import com.directoriocristiano.model.enums.BusinessStatus;
import com.directoriocristiano.model.enums.ChangeRequestStatus;
import com.directoriocristiano.model.enums.ChangeRequestType;
import com.directoriocristiano.model.enums.UserType;
import com.directoriocristiano.repository.BusinessChangeRequestRepository;
import com.directoriocristiano.repository.BusinessRepository;
import com.directoriocristiano.repository.ModerationEventRepository;
import com.directoriocristiano.repository.UserRepository;
import com.directoriocristiano.security.JwtProvider;
import com.directoriocristiano.service.ModerationServiceImpl;
import com.directoriocristiano.service.notify.ModerationNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** US4: solo moderadores entran a la bandeja; nadie decide sobre su propio negocio (FR-016, FR-019). */
@WebMvcTest(ModerationController.class)
@Import({SecurityConfig.class, ModerationServiceImpl.class})
class ModerationAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BusinessChangeRequestRepository changeRequestRepository;
    @MockitoBean
    private BusinessRepository businessRepository;
    @MockitoBean
    private ModerationEventRepository moderationEventRepository;
    @MockitoBean
    private ModerationNotifier notifier;
    @MockitoBean
    private UserRepository userRepository;
    @MockitoBean
    private JwtProvider jwtProvider;

    private User owner;
    private User moderator;
    private BusinessChangeRequest oldRequest;
    private BusinessChangeRequest recentRequest;

    @BeforeEach
    void setUp() {
        owner = user("duena@ejemplo.com", false);
        moderator = user("mod@ejemplo.com", true);

        Business published = Business.builder().id(UUID.randomUUID()).owner(owner).ownerName("Dueña")
                .name("Tutorías").category("Educación y Mentoría").zone("Zona Este").description("Clases")
                .contactPhone("+56 9 1111 2222").rating(BigDecimal.ZERO).status(BusinessStatus.published)
                .values(new ArrayList<>()).services(new ArrayList<>()).reviews(new ArrayList<>()).build();

        Map<String, Object> proposed = new LinkedHashMap<>();
        proposed.put("name", "Tutorías");
        proposed.put("category", "Educación y Mentoría");
        proposed.put("zone", "Zona Este");
        proposed.put("description", "Clases");
        proposed.put("contactPhone", "+56 9 7000 1111");
        proposed.put("values", List.of());
        proposed.put("services", List.of());

        oldRequest = request(published, ChangeRequestType.update, proposed, Duration.ofDays(4));
        recentRequest = request(published, ChangeRequestType.update, proposed, Duration.ofHours(5));

        when(changeRequestRepository.findByStatusOrderBySubmittedAtAsc(eq(ChangeRequestStatus.pending), any()))
                .thenReturn(new PageImpl<>(List.of(oldRequest, recentRequest)));
        when(changeRequestRepository.findById(oldRequest.getId())).thenReturn(Optional.of(oldRequest));
    }

    @Test
    void sinSesionDa401() throws Exception {
        mockMvc.perform(get("/api/v1/moderation/requests")).andExpect(status().isUnauthorized());
    }

    @Test
    void unClienteNoEntraALaBandeja() throws Exception {
        mockMvc.perform(get("/api/v1/moderation/requests").with(as(owner, "ROLE_CLIENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void laBandejaVieneDeLaMasAntiguaALaMasNuevaYMarcaLasAtrasadas() throws Exception {
        mockMvc.perform(get("/api/v1/moderation/requests").with(as(moderator, "ROLE_CLIENT", "ROLE_MODERATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(oldRequest.getId().toString()))
                .andExpect(jsonPath("$.content[0].daysWaiting").value(4))
                .andExpect(jsonPath("$.content[0].overdue").value(true))
                .andExpect(jsonPath("$.content[1].overdue").value(false))
                .andExpect(jsonPath("$.content[0].ownRequest").value(false));
    }

    @Test
    void estadoInvalidoDa400() throws Exception {
        mockMvc.perform(get("/api/v1/moderation/requests").param("status", "cualquiera")
                        .with(as(moderator, "ROLE_CLIENT", "ROLE_MODERATOR")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void elDetalleListaSoloLosCamposQueCambian() throws Exception {
        mockMvc.perform(get("/api/v1/moderation/requests/{id}", oldRequest.getId())
                        .with(as(moderator, "ROLE_CLIENT", "ROLE_MODERATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.changes.length()").value(1))
                .andExpect(jsonPath("$.changes[0].label").value("Teléfono"))
                .andExpect(jsonPath("$.changes[0].before").value("+56 9 1111 2222"))
                .andExpect(jsonPath("$.changes[0].after").value("+56 9 7000 1111"));
    }

    @Test
    void unModeradorDuenoNoPuedeAprobarSuPropioNegocio() throws Exception {
        owner.setModerator(true);
        mockMvc.perform(post("/api/v1/moderation/requests/{id}/approve", oldRequest.getId())
                        .with(as(owner, "ROLE_CLIENT", "ROLE_MODERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":0}"))
                .andExpect(status().isForbidden());
    }

    private RequestPostProcessor as(User user, String... roles) {
        return authentication(new UsernamePasswordAuthenticationToken(
                user, null, java.util.Arrays.stream(roles).map(SimpleGrantedAuthority::new).toList()));
    }

    private static User user(String email, boolean moderator) {
        return User.builder().id(UUID.randomUUID()).email(email).displayName(email)
                .userType(UserType.entrepreneur).moderator(moderator).build();
    }

    private BusinessChangeRequest request(Business business, ChangeRequestType type,
                                          Map<String, Object> proposed, Duration age) {
        BusinessChangeRequest request = BusinessChangeRequest.builder()
                .id(UUID.randomUUID()).business(business).type(type)
                .status(ChangeRequestStatus.pending).submittedBy(owner).build();
        request.setProposed(proposed);
        request.setSubmittedAt(Instant.now().minus(age));
        return request;
    }
}
