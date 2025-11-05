-- Pizzas
INSERT INTO pizzas (name, price, description) VALUES
    ('Margherita', 8.50, 'Classic tomato sauce, mozzarella, and fresh basil'),
    ('Pepperoni', 9.50, 'Tomato sauce, mozzarella, and pepperoni'),
    ('Hawaiian', 10.00, 'Tomato sauce, mozzarella, ham, and pineapple'),
    ('Quattro Formaggi', 11.50, 'Four cheese blend: mozzarella, gorgonzola, parmesan, and goat cheese'),
    ('Vegetariana', 10.50, 'Tomato sauce, mozzarella, bell peppers, mushrooms, onions, and olives'),
    ('Diavola', 10.00, 'Tomato sauce, mozzarella, spicy salami, and chili peppers'),
    ('Prosciutto', 11.00, 'Tomato sauce, mozzarella, prosciutto, and arugula'),
    ('Quattro Stagioni', 12.00, 'Four seasons: artichokes, mushrooms, ham, and olives');

-- Nutritional Info (@OneToOne relationship)
INSERT INTO nutritional_info (pizza_id, calories, protein, carbohydrates, fat) VALUES
    (1, 250, 12, 30, 10),
    (2, 300, 15, 32, 14),
    (3, 280, 13, 35, 11),
    (4, 320, 18, 28, 16),
    (5, 240, 10, 34, 8),
    (6, 310, 14, 33, 15),
    (7, 290, 16, 30, 12),
    (8, 330, 17, 36, 14);

-- Customers
INSERT INTO customers (name, email, phone, address, created_at, updated_at) VALUES
    ('John Doe', 'john@example.com', '+32471234567', 'Main Street 123, 1000 Brussels', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Jane Smith', 'jane@example.com', '+32479876543', 'High Street 45, 2000 Antwerp', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Bob Johnson', 'bob@example.com', '+32485555555', 'Park Lane 78, 9000 Ghent', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Alice Williams', 'alice@example.com', '+32498765432', 'Market Square 12, 3000 Leuven', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Charlie Brown', 'charlie@example.com', '+32477123456', 'Church Road 56, 8000 Bruges', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Orders (@ManyToOne from Order to Customer)
INSERT INTO orders (order_number, customer_id, order_date, total_amount, status, created_at, updated_at) VALUES
    ('ORD-2024-001', 1, CURRENT_TIMESTAMP - 2, 26.50, 'DELIVERED', CURRENT_TIMESTAMP - 2, CURRENT_TIMESTAMP - 2),
    ('ORD-2024-002', 2, CURRENT_TIMESTAMP - 1, 21.00, 'DELIVERED', CURRENT_TIMESTAMP - 1, CURRENT_TIMESTAMP - 1),
    ('ORD-2024-003', 1, CURRENT_TIMESTAMP, 11.50, 'PREPARING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('ORD-2024-004', 3, CURRENT_TIMESTAMP, 30.00, 'PENDING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('ORD-2024-005', 4, CURRENT_TIMESTAMP, 22.00, 'PENDING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('ORD-2024-006', 2, CURRENT_TIMESTAMP, 10.00, 'CONFIRMED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Order Lines (@ManyToOne from OrderLine to Order and Pizza)
INSERT INTO order_lines (order_id, pizza_id, quantity, unit_price, subtotal) VALUES
    -- Order 1 (John)
    (1, 1, 2, 8.50, 17.00),
    (1, 2, 1, 9.50, 9.50),
    -- Order 2 (Jane)
    (2, 3, 1, 10.00, 10.00),
    (2, 4, 1, 11.50, 11.50),
    -- Order 3 (John)
    (3, 4, 1, 11.50, 11.50),
    -- Order 4 (Bob)
    (4, 5, 1, 10.50, 10.50),
    (4, 6, 1, 10.00, 10.00),
    (4, 2, 1, 9.50, 9.50),
    -- Order 5 (Alice)
    (5, 7, 2, 11.00, 22.00),
    -- Order 6 (Jane)
    (6, 3, 1, 10.00, 10.00);

-- Customer Favorite Pizzas (@ManyToMany)
INSERT INTO customer_favorite_pizzas (customer_id, pizza_id) VALUES
    -- John favorites
    (1, 1),
    (1, 2),
    (1, 4),
    -- Jane favorites
    (2, 3),
    (2, 4),
    (2, 7),
    -- Bob favorites
    (3, 1),
    (3, 5),
    (3, 6),
    -- Alice favorites
    (4, 7),
    (4, 8),
    -- Charlie favorites
    (5, 1),
    (5, 2),
    (5, 3);
