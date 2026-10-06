-- La siembra de V3 fijó businesses.rating a mano (p. ej. 4.7 en un negocio sin
-- reseñas), así que el promedio mostrado no coincidía con las reseñas reales.
-- Se recalcula igual que BusinessServiceImpl.updateBusinessRating: promedio de
-- reviews.rating redondeado a 1 decimal, o 0 si el negocio no tiene reseñas.
UPDATE businesses b
SET rating = COALESCE(
    (SELECT ROUND(AVG(r.rating)::numeric, 1) FROM reviews r WHERE r.business_id = b.id),
    0.0
);
