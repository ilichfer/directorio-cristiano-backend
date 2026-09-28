-- La siembra original en V1 guardó estos nombres con la secuencia de escape
-- literal (p.ej. "ó") en vez del caracter UTF-8 real, lo que además
-- rompía el filtro por categoría (el texto libre en businesses.category no
-- coincidía con estos valores). Se corrige por coincidencia de patrón para
-- no depender de la representación exacta del texto corrupto, y se limpia
-- un registro de prueba.

UPDATE categories SET name = 'Construcción y Hogar' WHERE name LIKE 'Construcci%n y Hogar' AND name <> 'Construcción y Hogar';
UPDATE categories SET name = 'Educación y Mentoría' WHERE name LIKE 'Educaci%n y Mentor%a' AND name <> 'Educación y Mentoría';
UPDATE categories SET name = 'Alimentos y Repostería' WHERE name LIKE 'Alimentos y Reposter%a' AND name <> 'Alimentos y Repostería';
UPDATE categories SET name = 'Tecnología y Diseño' WHERE name LIKE 'Tecnolog%a y Dise%o' AND name <> 'Tecnología y Diseño';
UPDATE categories SET name = 'Artesanías y Confección' WHERE name LIKE 'Artesan%as y Confecci%n' AND name <> 'Artesanías y Confección';

DELETE FROM categories WHERE name LIKE 'Test-%';
