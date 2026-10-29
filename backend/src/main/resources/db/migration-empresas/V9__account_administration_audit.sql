CREATE TABLE account_admin_audit (
    id BIGSERIAL PRIMARY KEY,
    target_user_id BIGINT NOT NULL,
    actor_email VARCHAR(180) NOT NULL,
    previous_status VARCHAR(20) NOT NULL,
    new_status VARCHAR(20) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_account_admin_audit_target ON account_admin_audit(target_user_id,created_at DESC);
