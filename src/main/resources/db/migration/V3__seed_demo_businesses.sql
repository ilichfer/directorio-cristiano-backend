-- Negocios de ejemplo para que el directorio no se vea vacío durante el
-- desarrollo y la revisión visual. owner_id queda NULL (nadie los administra
-- todavía desde el panel de emprendedor).

INSERT INTO businesses (id, owner_id, name, owner_name, category, zone, description, slogan, cover_url, rating, featured, contact_phone, contact_whatsapp, contact_email, contact_website, contact_address) VALUES
('10000000-0000-0000-0000-000000000001', NULL, 'Pan y Sazón de Vida', 'Patricia de la Fe', 'Alimentos y Repostería', 'Zona Centro', 'Hago pancito amasado calientito y empanadas de pino en mi casa todas las tardes. Trato de dar precios muy bajos para bendecir las mesas de nuestros hermanos de la congregación y vecinos en tiempos duros, y compro harina integral cuando puedo.', 'Pancito amasado cada tarde, como en casa', 'https://images.unsplash.com/photo-1509440159596-0249088772ff?auto=format&fit=crop&q=80&w=800', 4.8, true, '+56 9 8123 4501', 'https://wa.me/56981234501', 'patricia.panvida@example.com', NULL, 'Pasaje Los Aromos 214, Zona Centro'),
('10000000-0000-0000-0000-000000000002', NULL, 'Servicios Técnicos Bethel', 'Francisco Hermosilla', 'Construcción y Hogar', 'Zona Sur', 'Sello filtraciones de cañerías, calderas y grifos, y destapo caños tapados de cocina y baño. Cobro lo que cuesta el repuesto de verdad y mi mano de obra sin engaños. Doy garantía por escrito.', 'Trabajo limpio, cobro justo', 'https://images.unsplash.com/photo-1621905251189-08b45d6a269e?auto=format&fit=crop&q=80&w=800', 4.6, false, '+56 9 8123 4502', 'https://wa.me/56981234502', 'francisco.bethel@example.com', NULL, 'Calle Los Claveles 345, Zona Sur'),
('10000000-0000-0000-0000-000000000003', NULL, 'Modas y Costuras Betania', 'Sara Ester Gómez', 'Artesanías y Confección', 'Zona Norte', 'Hago arreglos de ropa, cambio de cierres y basta de pantalones. Trabajo con hilos finos a máquina de pedal clásica y rematadora, para que las prendas duren lo más posible.', 'Terminación fina, hecha a mano', 'https://images.unsplash.com/photo-1556905055-8f358a7a47b2?auto=format&fit=crop&q=80&w=800', 4.9, true, '+56 9 8123 4503', 'https://wa.me/56981234503', 'sara.betania@example.com', NULL, 'Av. Independencia 1120, Zona Norte'),
('10000000-0000-0000-0000-000000000004', NULL, 'Tutorías Ebenezer', 'Daniel Contreras', 'Educación y Mentoría', 'Zona Este', 'Clases particulares de matemática y física para enseñanza media, presenciales u online. Preparo también para la PAES. Cada clase queda resumida en una guía corta para repasar solo.', 'Matemática sin miedo, paso a paso', 'https://images.unsplash.com/photo-1580582932707-520aed937b7b?auto=format&fit=crop&q=80&w=800', 4.7, false, '+56 9 8123 4504', 'https://wa.me/56981234504', 'daniel.ebenezer@example.com', NULL, 'Villa Las Encinas, Zona Este'),
('10000000-0000-0000-0000-000000000005', NULL, 'Diseño y Web Shalom', 'Camila Reyes', 'Tecnología y Diseño', 'Zona Centro', 'Armo sitios web simples y catálogos digitales para negocios pequeños que recién parten, con lenguaje claro y sin letra chica en la cotización. También ayudo a ordenar redes sociales.', 'Tu negocio, en línea y sin enredos', 'https://images.unsplash.com/photo-1499750310107-5fef28a66643?auto=format&fit=crop&q=80&w=800', 0.0, false, '+56 9 8123 4505', 'https://wa.me/56981234505', 'camila.shalom@example.com', 'https://camilareyes.example.com', 'Oficina 302, Edificio Central, Zona Centro'),
('10000000-0000-0000-0000-000000000006', NULL, 'Kinesiología Renuevo', 'Josué Fernández', 'Salud y Bienestar', 'Zona Oeste', 'Sesiones de kinesiología para dolor de espalda, rehabilitación post-operatoria y control postural. Primera evaluación siempre incluye un plan por escrito antes de cobrar el tratamiento completo.', 'Recuperación con plan claro', 'https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?auto=format&fit=crop&q=80&w=800', 5.0, false, '+56 9 8123 4506', 'https://wa.me/56981234506', 'josue.renuevo@example.com', NULL, 'Consultorio 4B, Paseo Las Torres, Zona Oeste');

INSERT INTO business_values (business_id, value) VALUES
('10000000-0000-0000-0000-000000000001', 'Comercio Justo'),
('10000000-0000-0000-0000-000000000001', 'Sencillez Humilde'),
('10000000-0000-0000-0000-000000000002', 'Cero Mentiras'),
('10000000-0000-0000-0000-000000000002', 'Puntualidad'),
('10000000-0000-0000-0000-000000000003', 'Amor Fraternal'),
('10000000-0000-0000-0000-000000000003', 'Terminación Fina'),
('10000000-0000-0000-0000-000000000004', 'Paciencia'),
('10000000-0000-0000-0000-000000000004', 'Claridad'),
('10000000-0000-0000-0000-000000000005', 'Precio Transparente'),
('10000000-0000-0000-0000-000000000006', 'Excelencia'),
('10000000-0000-0000-0000-000000000006', 'Trato Justo');

INSERT INTO services (business_id, name, price, description, sort_order) VALUES
('10000000-0000-0000-0000-000000000001', 'Pan amasado (kilo)', '$2.500', 'Recién horneado todas las tardes salvo domingo.', 1),
('10000000-0000-0000-0000-000000000001', 'Docena de empanadas de pino', '$14.000', 'Para encargos con un día de aviso.', 2),
('10000000-0000-0000-0000-000000000002', 'Destape de cañería', '$18.000', 'Incluye revisión de la causa, no solo el destape.', 1),
('10000000-0000-0000-0000-000000000002', 'Reparación de filtración', 'Desde $25.000', 'Precio final según repuesto, se cotiza antes de partir.', 2),
('10000000-0000-0000-0000-000000000003', 'Basta de pantalón', '$4.000', 'Entrega en 2 días hábiles.', 1),
('10000000-0000-0000-0000-000000000003', 'Cambio de cierre', '$6.000', 'Casacas, mochilas y bolsos.', 2),
('10000000-0000-0000-0000-000000000004', 'Clase particular (1 hora)', '$12.000', 'Matemática o física, presencial u online.', 1),
('10000000-0000-0000-0000-000000000004', 'Plan preparación PAES (mensual)', '$70.000', '4 clases + guías de repaso.', 2),
('10000000-0000-0000-0000-000000000005', 'Sitio web de 1 página', 'Desde $150.000', 'Incluye dominio del primer año.', 1),
('10000000-0000-0000-0000-000000000005', 'Catálogo digital', 'Desde $90.000', 'Hasta 30 productos, con fotos que envíes.', 2),
('10000000-0000-0000-0000-000000000006', 'Evaluación inicial', '$20.000', 'Incluye plan de tratamiento por escrito.', 1),
('10000000-0000-0000-0000-000000000006', 'Sesión de seguimiento', '$15.000', 'Duración 45 minutos.', 2);

INSERT INTO reviews (business_id, user_id, author_name, rating, comment, honesty, quality, punctuality, kindness, verified_client, created_at) VALUES
('10000000-0000-0000-0000-000000000001', NULL, 'Marcela Ibáñez', 5, 'El pan queda increíble y el precio no ha subido en meses aunque todo esté más caro. Se nota que es un trabajo hecho con cariño.', 5, 5, 5, 5, true, NOW() - INTERVAL '12 days'),
('10000000-0000-0000-0000-000000000001', NULL, 'Roberto Salinas', 5, 'Encargué empanadas para una reunión de la iglesia y llegaron justo a tiempo, buenísimas.', 5, 5, 4, 5, true, NOW() - INTERVAL '30 days'),
('10000000-0000-0000-0000-000000000002', NULL, 'Verónica Paz', 4, 'Vino rápido a revisar la filtración y cobró exactamente lo que había cotizado. Un poco justo con el horario pero cumplió.', 5, 4, 4, 4, true, NOW() - INTERVAL '5 days'),
('10000000-0000-0000-0000-000000000003', NULL, 'Andrea Rojas', 5, 'Le llevé un vestido de mi hija para el cierre y quedó como nuevo. Muy prolija en el detalle.', 5, 5, 5, 5, true, NOW() - INTERVAL '20 days'),
('10000000-0000-0000-0000-000000000006', NULL, 'Ignacio Torres', 5, 'Me explicó todo el plan antes de cobrar y en cuatro sesiones ya sentía harta mejoría en la espalda.', 5, 5, 5, 5, true, NOW() - INTERVAL '3 days');
