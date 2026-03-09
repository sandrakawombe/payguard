CREATE TABLE IF NOT EXISTS users (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email               VARCHAR(255) UNIQUE NOT NULL,
    password_hash       VARCHAR(255) NOT NULL,
    merchant_name       VARCHAR(255) NOT NULL,
    merchant_category   VARCHAR(50) NOT NULL,
    country             VARCHAR(3) NOT NULL,
    stripe_customer_id  VARCHAR(255) UNIQUE,
    role                VARCHAR(20) NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);