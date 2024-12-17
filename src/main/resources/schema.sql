CREATE TABLE wallet (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        user_id BIGINT NOT NULL,
                        wallet_id BIGINT NOT NULL,
                        usdt_balance DECIMAL(20, 8) NOT NULL,
                        btc_balance DECIMAL(20, 8) NOT NULL,
                        eth_balance DECIMAL(20, 8) NOT NULL,
                        status VARCHAR(255) NOT NULL,
                        version BIGINT,
                        created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                        updated_at TIMESTAMP NOT NULL,
                        created_by VARCHAR(255),
                        last_modified_by VARCHAR(255)
);
