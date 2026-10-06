package com.directoriocristiano.service;

import com.directoriocristiano.dto.BusinessRequest;
import com.directoriocristiano.dto.BusinessRequest.ServiceRequest;
import com.directoriocristiano.model.entity.Business;
import com.directoriocristiano.model.entity.ServiceItem;
import com.directoriocristiano.model.enums.BusinessStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** FR-012: qué se publica al instante y qué requiere aprobación. */
class BusinessChangeClassifierTest {

    private final BusinessChangeClassifier classifier = new BusinessChangeClassifier();
    private Business published;
    private ServiceItem clase;
    private ServiceItem taller;

    @BeforeEach
    void setUp() {
        published = Business.builder()
                .id(UUID.randomUUID())
                .name("Tutorías Ebenezer").category("Educación y Mentoría").zone("Zona Este")
                .description("Clases").contactPhone("+56 9 1111 2222").contactEmail("a@b.cl")
                .status(BusinessStatus.published)
                .values(new ArrayList<>(List.of("Paciencia")))
                .services(new ArrayList<>())
                .build();
        clase = service("Clase particular", "$12.000");
        taller = service("Taller grupal", "$8.000");
        published.getServices().addAll(List.of(clase, taller));
    }

    @Test
    void sinCambiosNoHayNada() {
        var result = classifier.classify(published, request("+56 9 1111 2222",
                List.of(same(clase), same(taller))));

        assertThat(result.needsApproval()).isFalse();
        assertThat(result.priceChanges()).isEmpty();
        assertThat(result.removedServices()).isEmpty();
    }

    @Test
    void textoVacioYNuloSonLoMismo() {
        published.setSlogan(null);
        var req = new BusinessRequest("Tutorías Ebenezer", "Educación y Mentoría", "Zona Este", "Clases",
                "   ", null, null, List.of("Paciencia"), "+56 9 1111 2222", null, "a@b.cl", "", null,
                List.of(same(clase), same(taller)));

        assertThat(classifier.classify(published, req).needsApproval()).isFalse();
    }

    @Test
    void cambioDePrecioEsInmediato() {
        var result = classifier.classify(published, request("+56 9 1111 2222", List.of(
                new ServiceRequest(clase.getId(), "Clase particular", "$15.000", null), same(taller))));

        assertThat(result.needsApproval()).isFalse();
        assertThat(result.priceChanges()).containsEntry(clase.getId(), "$15.000");
    }

    @Test
    void quitarUnServicioEsInmediato() {
        var result = classifier.classify(published, request("+56 9 1111 2222", List.of(same(clase))));

        assertThat(result.needsApproval()).isFalse();
        assertThat(result.removedServices()).containsExactly(taller.getId());
    }

    @Test
    void cambioDeTelefonoRequiereAprobacion() {
        var result = classifier.classify(published, request("+56 9 7000 1111",
                List.of(same(clase), same(taller))));

        assertThat(result.needsApproval()).isTrue();
        assertThat(result.proposed()).containsEntry("contactPhone", "+56 9 7000 1111");
    }

    @Test
    @SuppressWarnings("unchecked")
    void servicioNuevoRequiereAprobacionConSuPrecio() {
        var result = classifier.classify(published, request("+56 9 1111 2222", List.of(
                same(clase), same(taller), new ServiceRequest(null, "Reforzamiento", "$5.000", null))));

        assertThat(result.needsApproval()).isTrue();
        var services = (List<Map<String, Object>>) result.proposed().get("services");
        assertThat(services).hasSize(3);
        assertThat(services.get(2)).containsEntry("id", null).containsEntry("price", "$5.000");
        // Los precios de servicios existentes no viajan en la propuesta.
        assertThat(services.get(0)).doesNotContainKey("price");
    }

    @Test
    void renombrarUnServicioRequiereAprobacion() {
        var result = classifier.classify(published, request("+56 9 1111 2222", List.of(
                new ServiceRequest(clase.getId(), "Clase particular (90 min)", "$12.000", null), same(taller))));

        assertThat(result.needsApproval()).isTrue();
    }

    private static ServiceItem service(String name, String price) {
        return ServiceItem.builder().id(UUID.randomUUID()).name(name).price(price).build();
    }

    private static ServiceRequest same(ServiceItem service) {
        return new ServiceRequest(service.getId(), service.getName(), service.getPrice(), service.getDescription());
    }

    private static BusinessRequest request(String phone, List<ServiceRequest> services) {
        return new BusinessRequest("Tutorías Ebenezer", "Educación y Mentoría", "Zona Este", "Clases",
                null, null, null, List.of("Paciencia"), phone, null, "a@b.cl", null, null, services);
    }
}
