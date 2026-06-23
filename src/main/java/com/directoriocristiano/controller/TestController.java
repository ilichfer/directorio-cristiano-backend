package com.directoriocristiano.controller;

import com.directoriocristiano.dto.BusinessResponse;
import com.directoriocristiano.model.entity.Business;
import com.directoriocristiano.model.entity.Category;
import com.directoriocristiano.model.entity.Review;
import com.directoriocristiano.model.entity.ServiceItem;
import com.directoriocristiano.repository.BusinessRepository;
import com.directoriocristiano.repository.CategoryRepository;
import com.directoriocristiano.service.IBusinessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
public class TestController {

    private final CategoryRepository categoryRepository;
    private final BusinessRepository businessRepository;
    private final IBusinessService businessService;

    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        return ResponseEntity.ok(Map.of(
            "status", "pong",
            "timestamp", LocalDateTime.now().toString()
        ));
    }

    @GetMapping("/insert-category")
    public ResponseEntity<Map<String, Object>> insertCategory() {
        var testCategory = Category.builder()
            .name("Test-" + System.currentTimeMillis())
            .displayOrder(999)
            .build();
        var saved = categoryRepository.save(testCategory);
        return ResponseEntity.ok(Map.of(
            "status", "inserted",
            "id", saved.getId().toString(),
            "name", saved.getName()
        ));
    }

    @Transactional
    @GetMapping("/insert-business")
    public ResponseEntity<Map<String, Object>> insertBusiness() {
        var business = Business.builder()
            .name("Panadería la Fe")
            .ownerName("María González")
            .category("Alimentos y Repostería")
            .zone("Zona Centro")
            .description("Panadería artesanal que prepara pan amasado, empanadas de pino y repostería casera con ingredientes naturales. Atendemos con amor y esmero desde 2020.")
            .slogan("El sabor de la fe en cada bocado")
            .logoUrl("https://images.unsplash.com/photo-1542744094-3a31f103e35f?auto=format&fit=crop&q=80&w=200&h=200")
            .coverUrl("https://images.unsplash.com/photo-1460925895917-afdab827c52f?auto=format&fit=crop&q=80&w=800")
            .rating(BigDecimal.valueOf(4.8))
            .featured(true)
            .values(List.of("Comercio Justo", "Sabor Casero", "Atención con Amor", "Ingredientes Naturales"))
            .contactPhone("+56 9 1234 5678")
            .contactWhatsapp("https://wa.me/56912345678")
            .contactEmail("contacto@panaderialafe.cl")
            .contactWebsite("https://panaderialafe.cl")
            .contactAddress("Calle Los Claveles #345, Santiago Centro")
            .build();

        var service1 = ServiceItem.builder()
            .business(business)
            .name("Pan Amasado (1 kg)")
            .price("$4.500")
            .description("Pan amasado artesanal horneado en horno de barro")
            .sortOrder(1)
            .build();

        var service2 = ServiceItem.builder()
            .business(business)
            .name("Empanada de Pino (docena)")
            .price("$18.000")
            .description("Empanadas caseras con pino tradicional, pasas y aceitunas")
            .sortOrder(2)
            .build();

        var service3 = ServiceItem.builder()
            .business(business)
            .name("Torta Personalizada")
            .price("Desde $25.000")
            .description("Tortas para cumpleaños y eventos, con diseños personalizados")
            .sortOrder(3)
            .build();

        business.setServices(List.of(service1, service2, service3));

        var review = Review.builder()
            .business(business)
            .authorName("Carlos Muñoz")
            .rating(5)
            .comment("Excelente pan amasado, el mejor de la zona. Muy recomendable, siempre compro aquí.")
            .honesty(5)
            .quality(5)
            .punctuality(5)
            .kindness(5)
            .verifiedClient(true)
            .build();

        business.setReviews(List.of(review));

        var saved = businessRepository.save(business);

        return ResponseEntity.ok(Map.of(
            "status", "business_inserted",
            "id", saved.getId().toString(),
            "name", saved.getName(),
            "services_count", 3,
            "rating", saved.getRating().doubleValue(),
            "message", "Test business created. Call GET /api/v1/businesses/" + saved.getId() + " to verify"
        ));
    }

    @GetMapping("/business/{id}")
    public ResponseEntity<BusinessResponse> getBusiness(@PathVariable UUID id) {
        return ResponseEntity.ok(businessService.getById(id));
    }
}
