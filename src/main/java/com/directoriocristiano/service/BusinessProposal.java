package com.directoriocristiano.service;

import com.directoriocristiano.dto.BusinessRequest;
import com.directoriocristiano.model.entity.Business;
import com.directoriocristiano.model.entity.ServiceItem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Forma de {@code business_change_requests.proposed}: los campos que requieren aprobación
 * (FR-012), como mapa JSON. Los ids de servicio se guardan como texto.
 */
public final class BusinessProposal {

    public static final List<String> SCALAR_FIELDS = List.of(
            "name", "category", "zone", "description", "slogan", "logoUrl", "coverUrl",
            "contactPhone", "contactWhatsapp", "contactEmail", "contactWebsite", "contactAddress");

    private BusinessProposal() {
    }

    /** Foto completa de la ficha, incluidos precios (para negocios nuevos y para el historial). */
    public static Map<String, Object> snapshot(Business business) {
        Map<String, Object> map = scalarsOf(business);
        List<Map<String, Object>> services = new ArrayList<>();
        for (ServiceItem service : business.getServices()) {
            services.add(service(service.getId() == null ? null : service.getId().toString(),
                    service.getName(), service.getPrice(), service.getDescription()));
        }
        map.put("services", services);
        return map;
    }

    public static Map<String, Object> scalarsOf(Business business) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", normalize(business.getName()));
        map.put("category", normalize(business.getCategory()));
        map.put("zone", normalize(business.getZone()));
        map.put("description", normalize(business.getDescription()));
        map.put("slogan", normalize(business.getSlogan()));
        map.put("logoUrl", normalize(business.getLogoUrl()));
        map.put("coverUrl", normalize(business.getCoverUrl()));
        map.put("contactPhone", normalize(business.getContactPhone()));
        map.put("contactWhatsapp", normalize(business.getContactWhatsapp()));
        map.put("contactEmail", normalize(business.getContactEmail()));
        map.put("contactWebsite", normalize(business.getContactWebsite()));
        map.put("contactAddress", normalize(business.getContactAddress()));
        map.put("values", business.getValues() == null ? List.of() : new ArrayList<>(business.getValues()));
        return map;
    }

    public static Map<String, Object> scalarsOf(BusinessRequest request) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", normalize(request.name()));
        map.put("category", normalize(request.category()));
        map.put("zone", normalize(request.zone()));
        map.put("description", normalize(request.description()));
        map.put("slogan", normalize(request.slogan()));
        map.put("logoUrl", normalize(request.logoUrl()));
        map.put("coverUrl", normalize(request.coverUrl()));
        map.put("contactPhone", normalize(request.contactPhone()));
        map.put("contactWhatsapp", normalize(request.contactWhatsapp()));
        map.put("contactEmail", normalize(request.contactEmail()));
        map.put("contactWebsite", normalize(request.contactWebsite()));
        map.put("contactAddress", normalize(request.contactAddress()));
        map.put("values", request.values() == null ? List.of() : new ArrayList<>(request.values()));
        return map;
    }

    /** Copia los campos escalares y los valores de una propuesta sobre la ficha. */
    @SuppressWarnings("unchecked")
    public static void applyScalars(Map<String, Object> proposed, Business business) {
        business.setName(str(proposed.get("name")));
        business.setCategory(str(proposed.get("category")));
        business.setZone(str(proposed.get("zone")));
        business.setDescription(str(proposed.get("description")));
        business.setSlogan(str(proposed.get("slogan")));
        business.setLogoUrl(str(proposed.get("logoUrl")));
        business.setCoverUrl(str(proposed.get("coverUrl")));
        business.setContactPhone(str(proposed.get("contactPhone")));
        business.setContactWhatsapp(str(proposed.get("contactWhatsapp")));
        business.setContactEmail(str(proposed.get("contactEmail")));
        business.setContactWebsite(str(proposed.get("contactWebsite")));
        business.setContactAddress(str(proposed.get("contactAddress")));
        Object values = proposed.get("values");
        business.setValues(values instanceof List<?> list
                ? new ArrayList<>(list.stream().map(String::valueOf).toList())
                : new ArrayList<>());
    }

    public static Map<String, Object> service(String id, String name, String price, String description) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("name", normalize(name));
        if (price != null) {
            map.put("price", normalize(price));
        }
        map.put("description", normalize(description));
        return map;
    }

    /** Texto vacío y solo espacios cuentan como "sin dato", para no generar cambios fantasma. */
    public static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static boolean same(Object a, Object b) {
        return Objects.equals(a, b);
    }

    private static String str(Object value) {
        return value == null ? null : normalize(value.toString());
    }
}
