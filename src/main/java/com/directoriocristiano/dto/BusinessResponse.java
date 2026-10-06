package com.directoriocristiano.dto;

import com.directoriocristiano.model.entity.Business;
import com.directoriocristiano.model.entity.Review;
import com.directoriocristiano.model.entity.ServiceItem;
import com.directoriocristiano.model.enums.BusinessStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BusinessResponse(
        UUID id,
        UUID ownerId,
        String ownerName,
        String name,
        String category,
        String zone,
        String description,
        String slogan,
        String logoUrl,
        String coverUrl,
        BigDecimal rating,
        boolean featured,
        List<String> values,
        String contactPhone,
        String contactWhatsapp,
        String contactEmail,
        String contactWebsite,
        String contactAddress,
        boolean contactLocked,
        List<ServiceResponse> services,
        long reviewsCount,
        BusinessStatus status,
        Instant createdAt
) {
    /** Respuesta con contactos: solo para quien tiene sesión iniciada (principio II). */
    public static BusinessResponse from(Business business) {
        return from(business, true);
    }

    /**
     * Sin sesión, los datos de contacto se devuelven en {@code null} y {@code contactLocked = true}:
     * no basta con ocultarlos en pantalla (FR-026).
     */
    public static BusinessResponse from(Business business, boolean includeContact) {
        return new BusinessResponse(
                business.getId(),
                business.getOwner() != null ? business.getOwner().getId() : null,
                business.getOwnerName(),
                business.getName(),
                business.getCategory(),
                business.getZone(),
                business.getDescription(),
                business.getSlogan(),
                business.getLogoUrl(),
                business.getCoverUrl(),
                business.getRating(),
                business.isFeatured(),
                business.getValues(),
                includeContact ? business.getContactPhone() : null,
                includeContact ? business.getContactWhatsapp() : null,
                includeContact ? business.getContactEmail() : null,
                includeContact ? business.getContactWebsite() : null,
                includeContact ? business.getContactAddress() : null,
                !includeContact,
                business.getServices().stream().map(ServiceResponse::from).toList(),
                business.getReviews().size(),
                business.getStatus(),
                business.getCreatedAt()
        );
    }

    public record ServiceResponse(
            UUID id,
            String name,
            String price,
            String description,
            int sortOrder
    ) {
        public static ServiceResponse from(ServiceItem service) {
            return new ServiceResponse(
                    service.getId(),
                    service.getName(),
                    service.getPrice(),
                    service.getDescription(),
                    service.getSortOrder()
            );
        }
    }
}
