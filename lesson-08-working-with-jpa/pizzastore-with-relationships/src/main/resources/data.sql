-- Pizzas
INSERT INTO pizzas (name, price, description) VALUES
    ('Margherita', 8.50, 'Classic tomato sauce, mozzarella, and fresh basil'),
    ('Pepperoni', 9.50, 'Tomato sauce, mozzarella, and pepperoni'),
    ('Hawaiian', 10.00, 'Tomato sauce, mozzarella, ham, and pineapple'),
    ('Quattro Formaggi', 11.50, 'Four cheese blend'),
    ('Vegetariana', 10.50, 'Vegetables and mozzarella');

-- Nutritional Info (@OneToOne)
INSERT INTO nutritional_info (pizza_id, calories, protein, carbohydrates, fat) VALUES
    (1, 250, 12, 30, 10),
    (2, 300, 15, 32, 14),
    (3, 280, 13, 35, 11),
    (4, 320, 18, 28, 16),
    (5, 240, 10, 34, 8);

-- Customers
INSERT INTO customers (name, email, phone, address) VALUES
    ('John Doe', 'john@example.com', '0471234567', 'Main Street 123, Brussels'),
    ('Jane Smith', 'jane@example.com', '0479876543', 'High Street 45, Antwerp'),
    ('Bob Johnson', 'bob@example.com', '0485555555', 'Park Lane 78, Ghent');

-- Orders (@ManyToOne from Order to Customer)
INSERT INTO orders (order_number, customer_id, order_date, total_amount, status) VALUES
    ('ORD-2024-001', 1, CURRENT_TIMESTAMP, 26.50, 'DELIVERED'),
    ('ORD-2024-002', 2, CURRENT_TIMESTAMP, 18.00, 'PENDING'),
    ('ORD-2024-003', 1, CURRENT_TIMESTAMP, 11.50, 'PREPARING');

-- Order Lines
INSERT INTO order_lines (order_id, pizza_id, quantity, unit_price, subtotal) VALUES
    (1, 1, 2, 8.50, 17.00),
    (1, 2, 1, 9.50, 9.50),
    (2, 3, 1, 10.00, 10.00),
    (2, 1, 1, 8.50, 8.50),
    (3, 4, 1, 11.50, 11.50);

-- Customer Favorite Pizzas (@ManyToMany)
INSERT INTO customer_favorite_pizzas (customer_id, pizza_id) VALUES
    (1, 1),  -- John favorites Margherita
    (1, 2),  -- John favorites Pepperoni
    (2, 3),  -- Jane favorites Hawaiian
    (2, 4),  -- Jane favorites Quattro Formaggi
    (3, 1),  -- Bob favorites Margherita
    (3, 5);  -- Bob favorites Vegetariana
