CREATE TABLE IF NOT EXISTS fraud_audit_log (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id        UUID NOT NULL,
    merchant_id           UUID NOT NULL,
    fraud_score           DECIMAL(4,3) NOT NULL,
    decision              VARCHAR(20) NOT NULL,
    model_version         VARCHAR(50) NOT NULL,
    features_json         JSONB NOT NULL,
    contributing_factors  JSONB NOT NULL,
    latency_ms            INTEGER NOT NULL,
    fallback_used         BOOLEAN NOT NULL DEFAULT false,
    created_at            TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_fraud_audit_transaction ON fraud_audit_log(transaction_id);
CREATE INDEX IF NOT EXISTS idx_fraud_audit_merchant ON fraud_audit_log(merchant_id);
CREATE INDEX IF NOT EXISTS idx_fraud_audit_created ON fraud_audit_log(created_at);