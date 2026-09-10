-- Pizzas (with audit fields - will be set by JPA Auditing)
INSERT INTO pizzas (name, description, price, image_url, available, created_at, updated_at) VALUES
('Margherita', 'Classic tomato sauce, fresh mozzarella, basil, and extra virgin olive oil', 8.99, 'https://images.unsplash.com/photo-1574071318508-1cdbab80d002', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Pepperoni', 'Tomato sauce, mozzarella, and spicy pepperoni slices', 10.99, 'https://images.unsplash.com/photo-1628840042765-356cda07504e', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Quattro Formaggi', 'Four cheese blend: mozzarella, gorgonzola, parmesan, and fontina', 11.99, 'https://images.unsplash.com/photo-1571997478779-2adcbbe9ab2f', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Vegetariana', 'Fresh vegetables: bell peppers, mushrooms, onions, tomatoes, and olives', 9.99, 'https://images.unsplash.com/photo-1627626775846-122c3f8e3e3b', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Diavola', 'Spicy salami, hot peppers, tomato sauce, and mozzarella', 12.99, 'https://images.unsplash.com/photo-1593560708920-61dd98c46a4e', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Hawaii', 'Ham, pineapple, tomato sauce, and mozzarella', 11.49, 'https://images.unsplash.com/photo-1565299624946-b28f40a0ae38', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Funghi', 'Mushrooms, tomato sauce, mozzarella, and fresh herbs', 10.49, 'https://images.unsplash.com/photo-1513104890138-7c749659a591', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Prosciutto', 'Italian prosciutto, arugula, parmesan shavings, and mozzarella', 13.99, 'https://images.unsplash.com/photo-1571407970349-bc81e7e96a47', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Marinara', 'Tomato sauce, garlic, oregano, and extra virgin olive oil (no cheese)', 7.99, null, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Capricciosa', 'Ham, mushrooms, artichokes, olives, tomato sauce, and mozzarella', 12.49, 'https://images.unsplash.com/photo-1595854341625-f33ee10dbf94', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BBQ Chicken', 'Grilled chicken, BBQ sauce, red onions, and mozzarella', 13.49, null, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Tonno', 'Tuna, red onions, capers, tomato sauce, and mozzarella', 11.99, null, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Nutritional Info for Pizzas
INSERT INTO nutritional_info (calories, protein, carbohydrates, fat, pizza_id) VALUES
(266, 11.0, 33.0, 10.0, 1),  -- Margherita
(298, 13.5, 36.0, 12.5, 2),  -- Pepperoni
(320, 15.0, 35.0, 14.0, 3),  -- Quattro Formaggi
(245, 9.0, 38.0, 8.5, 4),    -- Vegetariana
(310, 14.0, 36.0, 13.0, 5),  -- Diavola
(275, 12.0, 39.0, 9.5, 6),   -- Hawaii
(255, 10.5, 35.0, 9.0, 7),   -- Funghi
(295, 16.0, 33.0, 11.5, 8),  -- Prosciutto
(200, 6.0, 35.0, 5.0, 9),    -- Marinara
(285, 13.0, 37.0, 10.5, 10), -- Capricciosa
(310, 18.0, 34.0, 12.0, 11), -- BBQ Chicken
(270, 14.0, 34.0, 9.5, 12);  -- Tonno

-- Customers with BCrypt encoded passwords
-- Password format: "password123" encoded with BCrypt
-- For testing: email: emma.johnson@example.com, password: password123
-- For admin: email: admin@pizzastore.be, password: password123
INSERT INTO customers (name, email, password, phone, address, role, created_at, updated_at) VALUES
('Emma Johnson', 'emma.johnson@example.com', '$2a$10$wvw30spxLOR1gV/NYh86ruw8J1rPa8MvkZwG0ru7VuRECMfARo0ri', '+32 470 12 34 56', 'Rue de la Loi 123, 1000 Brussels', 'CUSTOMER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Liam Smith', 'liam.smith@example.com', '$2a$10$wvw30spxLOR1gV/NYh86ruw8J1rPa8MvkZwG0ru7VuRECMfARo0ri', '+32 471 23 45 67', 'Meir 45, 2000 Antwerp', 'CUSTOMER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Olivia Brown', 'olivia.brown@example.com', '$2a$10$wvw30spxLOR1gV/NYh86ruw8J1rPa8MvkZwG0ru7VuRECMfARo0ri', '+32 472 34 56 78', 'Korenmarkt 12, 9000 Ghent', 'CUSTOMER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Noah Davis', 'noah.davis@example.com', '$2a$10$wvw30spxLOR1gV/NYh86ruw8J1rPa8MvkZwG0ru7VuRECMfARo0ri', '+32 473 45 67 89', 'Grand Place 1, 7000 Mons', 'CUSTOMER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Ava Wilson', 'ava.wilson@example.com', '$2a$10$wvw30spxLOR1gV/NYh86ruw8J1rPa8MvkZwG0ru7VuRECMfARo0ri', '+32 474 56 78 90', 'Boulevard Tirou 89, 6000 Charleroi', 'CUSTOMER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Admin User', 'admin@pizzastore.be', '$2a$10$wvw30spxLOR1gV/NYh86ruw8J1rPa8MvkZwG0ru7VuRECMfARo0ri', '+32 475 67 89 01', 'Headquarters, 1000 Brussels', 'ADMIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Favorite Pizzas (Many-to-Many relationship)
INSERT INTO customer_favorite_pizzas (customer_id, pizza_id) VALUES
(1, 1), (1, 3), (1, 5),  -- Emma likes Margherita, Quattro Formaggi, Diavola
(2, 2), (2, 5), (2, 11), -- Liam likes Pepperoni, Diavola, BBQ Chicken
(3, 1), (3, 4), (3, 9),  -- Olivia likes Margherita, Vegetariana, Marinara
(4, 2), (4, 6), (4, 8),  -- Noah likes Pepperoni, Hawaii, Prosciutto
(5, 3), (5, 7), (5, 10); -- Ava likes Quattro Formaggi, Funghi, Capricciosa

-- Orders
INSERT INTO orders (order_number, order_date, total_amount, status, customer_id, created_at, updated_at) VALUES
('ORD-20240115-00001', '2024-01-15 12:30:00', 20.98, 'DELIVERED', 1, '2024-01-15 12:30:00', '2024-01-15 14:15:00'),
('ORD-20240115-00002', '2024-01-15 14:00:00', 22.98, 'DELIVERED', 2, '2024-01-15 14:00:00', '2024-01-15 15:45:00'),
('ORD-20240116-00003', '2024-01-16 10:15:00', 34.97, 'DELIVERED', 3, '2024-01-16 10:15:00', '2024-01-16 12:30:00'),
('ORD-20240116-00004', '2024-01-16 18:45:00', 24.98, 'DELIVERED', 1, '2024-01-16 18:45:00', '2024-01-16 20:15:00'),
('ORD-20240117-00005', '2024-01-17 11:00:00', 13.99, 'DELIVERED', 4, '2024-01-17 11:00:00', '2024-01-17 12:45:00'),
('ORD-20240117-00006', '2024-01-17 19:30:00', 36.47, 'DELIVERED', 5, '2024-01-17 19:30:00', '2024-01-17 21:00:00'),
('ORD-20240118-00007', '2024-01-18 12:00:00', 21.98, 'READY', 2, '2024-01-18 12:00:00', '2024-01-18 12:45:00'),
('ORD-20240118-00008', '2024-01-18 13:15:00', 32.97, 'PREPARING', 3, '2024-01-18 13:15:00', '2024-01-18 13:30:00'),
('ORD-20240118-00009', '2024-01-18 17:00:00', 25.48, 'CONFIRMED', 1, '2024-01-18 17:00:00', '2024-01-18 17:05:00'),
('ORD-20240118-00010', '2024-01-18 19:00:00', 18.98, 'PENDING', 4, '2024-01-18 19:00:00', '2024-01-18 19:00:00');

-- Order Lines (linking orders to pizzas)
-- Order 1: Emma - Margherita x2, Quattro Formaggi x1
INSERT INTO order_lines (order_id, pizza_id, quantity, unit_price, subtotal) VALUES
(1, 1, 2, 8.99, 17.98),
(1, 3, 1, 11.99, 11.99);

-- Order 2: Liam - Pepperoni x1, Diavola x1
INSERT INTO order_lines (order_id, pizza_id, quantity, unit_price, subtotal) VALUES
(2, 2, 1, 10.99, 10.99),
(2, 5, 1, 12.99, 12.99);

-- Order 3: Olivia - Vegetariana x2, Marinara x2
INSERT INTO order_lines (order_id, pizza_id, quantity, unit_price, subtotal) VALUES
(3, 4, 2, 9.99, 19.98),
(3, 9, 2, 7.99, 15.98);

-- Order 4: Emma - Pepperoni x1, Diavola x1
INSERT INTO order_lines (order_id, pizza_id, quantity, unit_price, subtotal) VALUES
(4, 2, 1, 10.99, 10.99),
(4, 5, 1, 12.99, 12.99);

-- Order 5: Noah - Prosciutto x1
INSERT INTO order_lines (order_id, pizza_id, quantity, unit_price, subtotal) VALUES
(5, 8, 1, 13.99, 13.99);

-- Order 6: Ava - Quattro Formaggi x2, Capricciosa x1
INSERT INTO order_lines (order_id, pizza_id, quantity, unit_price, subtotal) VALUES
(6, 3, 2, 11.99, 23.98),
(6, 10, 1, 12.49, 12.49);

-- Order 7: Liam - Pepperoni x2
INSERT INTO order_lines (order_id, pizza_id, quantity, unit_price, subtotal) VALUES
(7, 2, 2, 10.99, 21.98);

-- Order 8: Olivia - Margherita x1, Funghi x1, Hawaii x1
INSERT INTO order_lines (order_id, pizza_id, quantity, unit_price, subtotal) VALUES
(8, 1, 1, 8.99, 8.99),
(8, 7, 1, 10.49, 10.49),
(8, 6, 1, 11.49, 11.49);

-- Order 9: Emma - BBQ Chicken x1, Tonno x1
INSERT INTO order_lines (order_id, pizza_id, quantity, unit_price, subtotal) VALUES
(9, 11, 1, 13.49, 13.49),
(9, 12, 1, 11.99, 11.99);

-- Order 10: Noah - Margherita x1, Vegetariana x1
INSERT INTO order_lines (order_id, pizza_id, quantity, unit_price, subtotal) VALUES
(10, 1, 1, 8.99, 8.99),
(10, 4, 1, 9.99, 9.99);
