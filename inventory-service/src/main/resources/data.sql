INSERT INTO products (id, product_name, available_quantity, price) VALUES
                                                                   (1,  'Keyboard',     20,  120.00),
                                                                   (2,  'Mouse',        30,   45.00),
                                                                   (3,  'Monitor',      10,  350.00),
                                                                   (4,  'Webcam',       15,   85.00),
                                                                   (5,  'Headphones',   25,  150.00),
                                                                   (6,  'USB-C Cable', 100,   15.00),
                                                                   (7,  'Laptop Stand', 12,   60.00),
                                                                   (8,  'Microphone',    8,  200.00),
                                                                   (9,  'SSD 1TB',      18,  110.00),
                                                                   (10, 'Laptop Bag',   22,   75.00)
    ON CONFLICT (id) DO UPDATE
                            SET product_name = EXCLUDED.product_name,
                            available_quantity = EXCLUDED.available_quantity,
                            price = EXCLUDED.price;