INSERT INTO user_balances (user_id, balance)
VALUES
    (1, 10000.00),
    (2, 5000.00),
    (3, 1000.00),
    (4, 0.00),
    (5, 550.00),
    (6, 430.00),
    (7, 4000.00),
    (8, 2000.00),
    (9, 1000.00),
    (10, 100.00)
    ON CONFLICT (user_id) DO NOTHING;