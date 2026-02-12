
INSERT INTO users (id, email, password_hash, role)
VALUES
    ('550e8400-e29b-41d4-a716-446655440001', 'admin@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVQsUi', 'ADMIN'),
    ('550e8400-e29b-41d4-a716-446655440002', 'user1@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVQsUi', 'USER'),
    ('550e8400-e29b-41d4-a716-446655440003', 'user2@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVQsUi', 'USER');

-- Пароль для всех: "password123"

--------------------------------------------------
-- PRODUCTS
--------------------------------------------------
INSERT INTO products (id, name, description, price, stock)
VALUES
    ('660e8400-e29b-41d4-a716-446655440001', 'Laptop', 'Gaming laptop', 1200.00, 10),
    ('660e8400-e29b-41d4-a716-446655440002', 'Phone', 'Smartphone', 800.00, 20),
    ('660e8400-e29b-41d4-a716-446655440003', 'Headphones', 'Wireless headphones', 150.00, 50);

--------------------------------------------------
-- ORDERS (используем только допустимые статусы)
--------------------------------------------------
INSERT INTO orders (id, user_id, status, total_price)
VALUES
    ('770e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440002', 'PENDING', 1350.00),
    ('770e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440003', 'PENDING', 800.00);

--------------------------------------------------
-- ORDER ITEMS
--------------------------------------------------
INSERT INTO order_items (id, order_id, product_id, quantity, price)
VALUES
    ('880e8400-e29b-41d4-a716-446655440001', '770e8400-e29b-41d4-a716-446655440001', '660e8400-e29b-41d4-a716-446655440001', 1, 1200.00),
    ('880e8400-e29b-41d4-a716-446655440002', '770e8400-e29b-41d4-a716-446655440001', '660e8400-e29b-41d4-a716-446655440003', 1, 150.00),
    ('880e8400-e29b-41d4-a716-446655440003', '770e8400-e29b-41d4-a716-446655440002', '660e8400-e29b-41d4-a716-446655440002', 1, 800.00);

--------------------------------------------------
-- AUDIT LOGS
--------------------------------------------------
INSERT INTO audit_logs (id, user_id, action, entity_type, entity_id, details)
VALUES
    ('990e8400-e29b-41d4-a716-446655440001',
     '550e8400-e29b-41d4-a716-446655440002',
     'ORDER_CREATED',
     'ORDER',
     '770e8400-e29b-41d4-a716-446655440001',
     'Order created with total 1350.00'),

    ('990e8400-e29b-41d4-a716-446655440002',
     '550e8400-e29b-41d4-a716-446655440003',
     'ORDER_CREATED',
     'ORDER',
     '770e8400-e29b-41d4-a716-446655440002',
     'Order created with total 800.00');