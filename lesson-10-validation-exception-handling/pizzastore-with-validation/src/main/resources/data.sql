-- Dummy data for the pizzastore domain model, loaded automatically by Spring Boot's
-- SQL initializer (spring.jpa.defer-datasource-initialization=true) after Hibernate
-- has created the schema from the @Entity classes. Same shape as the final pizzastore
-- project's seed data, trimmed to what the domain/repository packages need here.

-- Pizzas
INSERT INTO pizzas (name, description, price, image_url, available, created_at, updated_at) VALUES
('Margherita', 'Classic tomato sauce, fresh mozzarella, basil, and extra virgin olive oil', 8.99, 'https://images.unsplash.com/photo-1574071318508-1cdbab80d002', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Pepperoni', 'Tomato sauce, mozzarella, and spicy pepperoni slices', 10.99, 'https://images.unsplash.com/photo-1628840042765-356cda07504e', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Quattro Formaggi', 'Four cheese blend: mozzarella, gorgonzola, parmesan, and fontina', 11.99, 'https://images.unsplash.com/photo-1571997478779-2adcbbe9ab2f', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Vegetariana', 'Fresh vegetables: bell peppers, mushrooms, onions, tomatoes, and olives', 9.99, 'https://images.unsplash.com/photo-1627626775846-122c3f8e3e3b', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Diavola', 'Spicy salami, hot peppers, tomato sauce, and mozzarella', 12.99, 'https://images.unsplash.com/photo-1593560708920-61dd98c46a4e', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Hawaii', 'Ham, pineapple, tomato sauce, and mozzarella', 11.49, 'https://images.unsplash.com/photo-1565299624946-b28f40a0ae38', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Nutritional Info (@OneToOne, owning side has the FK)
INSERT INTO nutritional_info (calories, protein, carbohydrates, fat, pizza_id) VALUES
(266, 11.0, 33.0, 10.0, 1),  -- Margherita
(298, 13.5, 36.0, 12.5, 2),  -- Pepperoni
(320, 15.0, 35.0, 14.0, 3),  -- Quattro Formaggi
(245, 9.0, 38.0, 8.5, 4),    -- Vegetariana
(310, 14.0, 36.0, 13.0, 5),  -- Diavola
(275, 12.0, 39.0, 9.5, 6);   -- Hawaii

-- Customers (password column is part of the copied Customer entity; BCrypt hash for "password123")
INSERT INTO customers (name, email, password, phone, address, role, created_at, updated_at) VALUES
('Emma Johnson', 'emma.johnson@example.com', '$2a$10$wvw30spxLOR1gV/NYh86ruw8J1rPa8MvkZwG0ru7VuRECMfARo0ri', '+32 470 12 34 56', 'Rue de la Loi 123, 1000 Brussels', 'CUSTOMER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Liam Smith', 'liam.smith@example.com', '$2a$10$wvw30spxLOR1gV/NYh86ruw8J1rPa8MvkZwG0ru7VuRECMfARo0ri', '+32 471 23 45 67', 'Meir 45, 2000 Antwerp', 'CUSTOMER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Admin User', 'admin@pizzastore.be', '$2a$10$wvw30spxLOR1gV/NYh86ruw8J1rPa8MvkZwG0ru7VuRECMfARo0ri', '+32 475 67 89 01', 'Headquarters, 1000 Brussels', 'ADMIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Favorite Pizzas (@ManyToMany join table)
INSERT INTO customer_favorite_pizzas (customer_id, pizza_id) VALUES
(1, 1), (1, 3), (1, 5),  -- Emma likes Margherita, Quattro Formaggi, Diavola
(2, 2), (2, 5);          -- Liam likes Pepperoni, Diavola

-- Orders
INSERT INTO orders (order_number, order_date, total_amount, status, customer_id, created_at, updated_at) VALUES
('ORD-20240115-00001', '2024-01-15 12:30:00', 20.98, 'DELIVERED', 1, '2024-01-15 12:30:00', '2024-01-15 14:15:00'),
('ORD-20240115-00002', '2024-01-15 14:00:00', 22.98, 'DELIVERED', 2, '2024-01-15 14:00:00', '2024-01-15 15:45:00'),
('ORD-20240118-00003', '2024-01-18 17:00:00', 25.48, 'CONFIRMED', 1, '2024-01-18 17:00:00', '2024-01-18 17:05:00');

-- Order Lines
INSERT INTO order_lines (order_id, pizza_id, quantity, unit_price, subtotal) VALUES
(1, 1, 2, 8.99, 17.98),
(1, 3, 1, 11.99, 11.99),
(2, 2, 1, 10.99, 10.99),
(2, 5, 1, 12.99, 12.99),
(3, 4, 1, 9.99, 9.99),
(3, 6, 1, 11.49, 11.49);
