USE points_db;

-- The account is the current source of truth for the available balance.
CREATE TABLE IF NOT EXISTS point_accounts (
    user_id    BIGINT UNSIGNED NOT NULL,
    balance    BIGINT UNSIGNED NOT NULL DEFAULT 0,
    version    BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'optimistic lock version',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='point accounts';

-- Immutable ledger. A unique business number makes all point commands idempotent.
CREATE TABLE IF NOT EXISTS point_ledger (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id        BIGINT UNSIGNED NOT NULL,
    business_no    VARCHAR(64) NOT NULL COMMENT 'idempotency key',
    operation_type VARCHAR(32) NOT NULL COMMENT 'EARN, SPEND, ADJUST, etc.',
    change_amount  BIGINT NOT NULL COMMENT 'positive earns, negative spends',
    balance_after  BIGINT UNSIGNED NOT NULL,
    remark         VARCHAR(256) NULL,
    created_at     DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_point_ledger_business_no (business_no),
    KEY idx_point_ledger_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='point ledger';
