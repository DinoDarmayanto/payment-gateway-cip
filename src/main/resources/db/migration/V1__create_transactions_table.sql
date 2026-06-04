CREATE TABLE IF NOT EXISTS transactions (
    id UUID PRIMARY KEY,
    order_id VARCHAR(100) NOT NULL UNIQUE,
    channel VARCHAR(50) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    account VARCHAR(50) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'IDR',
    payment_method VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    corebank_reference VARCHAR(100),
    biller_reference VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);