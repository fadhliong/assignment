INSERT INTO wallet (
    id,
    user_id,
    wallet_id,
    usdt_balance,
    btc_balance,
    eth_balance,
    status,
    version,
    created_at,
    updated_at,
    created_by,
    last_modified_by
) VALUES (
             1, 12345, 123, 150000.00000000, 0.00000000, 0.00000000, 'ACTIVE', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'mockUser', 'mockUser'
         );