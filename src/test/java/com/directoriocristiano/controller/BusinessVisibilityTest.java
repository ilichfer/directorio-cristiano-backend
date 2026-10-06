package com.directoriocristiano.controller;

import com.directoriocristiano.config.SecurityConfig;
import com.directoriocristiano.model.entity.Business;
import com.directoriocristiano.model.entity.User;
import com.directoriocristiano.model.enums.BusinessStatus;
import com.directoriocristiano.model.enums.UserType;
import com.directoriocristiano.repository.BusinessChangeRequestRepository;
import com.directoriocristiano.repository.BusinessRepository;
import com.directoriocristiano.repository.ModerationEventRepository;
import com.directoriocristiano.repository.ReviewRepository;
import com.directoriocristiano.repository.UserRepository;
import com.directoriocristiano.security.JwtProvider;
import com.directoriocristiano.service.BusinessServiceImpl;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Principio II / FR-026 / FR-007: los contactos no salen de la API sin sesión, y los negocios no
 * publicados no existen para el público.
 */
@WebMvcTest(BusinessController.class)
@Import({SecurityConfig.class, BusinessServiceImpl.class})
class BusinessVisibilityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BusinessRepository businessRepository;
    @MockitoBean
    private ReviewRepository reviewRepository;
    @MockitoBean
    private BusinessChangeRequestRepository changeRequestRepository;
    @MockitoBean
    private ModerationEventRepository moderationEventRepository;
    @MockitoBean
    private UserRepository userRepository;
    @MockitoBean
    private JwtProvider jwtProvider;

    private User owner;
    private User stranger;
    private Business published;
    private Business draft;

    @BeforeEach
    void setUp() {
        owner = user("duena@ejemplo.com");
        stranger = user("vecino@ejemplo.com");
        published = business(owner, BusinessStatus.published);
        draft = business(owner, BusinessStatus.draft);

        when(businessRepository.searchBusinesses(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(published)));
        when(businessRepository.findById(published.getId())).thenReturn(Optional.of(published));
        when(businessRepository.findById(draft.getId())).thenReturn(Optional.of(draft));
    }

    @Test
    void listadoSinSesionNoEntregaContactos() throws Exception {
        mockMvc.perform(get("/api/v1/businesses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].contactLocked").value(true))
                .andExpect(jsonPath("$.content[0].contactPhone").doesNotExist())
                .andExpect(jsonPath("$.content[0].contactWhatsapp").doesNotExist())
                .andExpect(jsonPath("$.content[0].contactEmail").doesNotExist())
                .andExpect(jsonPath("$.content[0].contactAddress").doesNotExist());
    }

    @Test
    void listadoConSesionEntregaContactos() throws Exception {
        mockMvc.perform(get("/api/v1/businesses").with(as(stranger)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].contactLocked").value(false))
                .andExpect(jsonPath("$.content[0].contactPhone").value("+56 9 1111 2222"));
    }

    @Test
    void fichaSinSesionNoEntregaContactos() throws Exception {
        mockMvc.perform(get("/api/v1/businesses/{id}", published.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contactLocked").value(true))
                .andExpect(jsonPath("$.contactEmail").doesNotExist());
    }

    @Test
    void borradorNoExisteParaElPublicoNiParaTerceros() throws Exception {
        mockMvc.perform(get("/api/v1/businesses/{id}", draft.getId()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/businesses/{id}", draft.getId()).with(as(stranger)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/businesses/{id}/reviews", draft.getId()).with(as(stranger)))
                .andExpect(status().isNotFound());
    }

    @Test
    void elDuenoVeSuBorrador() throws Exception {
        mockMvc.perform(get("/api/v1/businesses/{id}", draft.getId()).with(as(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("draft"));
    }

    @Test
    void misNegociosSinSesionDa401() throws Exception {
        mockMvc.perform(get("/api/v1/businesses/mine"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void elDuenoNoPuedeResenarSuPropioNegocio() throws Exception {
        String review = """
                {"authorName":"Dueña","rating":5,"comment":"Excelente","honesty":5,
                 "quality":5,"punctuality":5,"kindness":5}
                """;
        mockMvc.perform(post("/api/v1/businesses/{id}/reviews", published.getId())
                        .with(as(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(review))
                .andExpect(status().isForbidden());
    }

    private RequestPostProcessor as(User user) {
        return authentication(new UsernamePasswordAuthenticationToken(
                user, null, List.of(new SimpleGrantedAuthority("ROLE_CLIENT"))));
    }

    private static User user(String email) {
        return User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .displayName(email)
                .userType(UserType.buyer)
                .build();
    }

    private static Business business(User owner, BusinessStatus status) {
        return Business.builder()
                .id(UUID.randomUUID())
                .owner(owner)
                .ownerName(owner.getDisplayName())
                .name("Negocio " + status)
                .category("Servicios Profesionales")
                .zone("Zona Centro")
                .description("Descripción")
                .rating(BigDecimal.ZERO)
                .status(status)
                .values(new ArrayList<>())
                .services(new ArrayList<>())
                .reviews(new ArrayList<>())
                .contactPhone("+56 9 1111 2222")
                .contactWhatsapp("+56 9 1111 2222")
                .contactEmail("contacto@negocio.cl")
                .contactAddress("Calle 123")
                .build();
    }
}
