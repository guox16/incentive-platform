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

-- A reservation freezes points for a cross-service operation before it is confirmed or compensated.
-- business_id is the stable idempotency key shared by reserve/confirm/cancel retries.
CREATE TABLE IF NOT EXISTS point_reservations (
    id                       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_id              BIGINT UNSIGNED NOT NULL COMMENT 'stable idempotency key',
    user_id                  BIGINT UNSIGNED NOT NULL,
    amount                   BIGINT UNSIGNED NOT NULL COMMENT 'reserved points',
    source                   VARCHAR(32) NOT NULL COMMENT 'business source, e.g. LOTTERY or REDEMPTION',
    remark                   VARCHAR(200) NULL,
    status                   VARCHAR(16) NOT NULL DEFAULT 'RESERVED'
        COMMENT 'RESERVED, CONFIRMED, CANCELLED, EXPIRED',
    confirmed_transaction_id BIGINT UNSIGNED NULL COMMENT 'point transaction created on confirmation',
    expires_at               DATETIME(3) NOT NULL COMMENT 'deadline for recovery or expiration',
    confirmed_at             DATETIME(3) NULL,
    cancelled_at             DATETIME(3) NULL,
    expired_at               DATETIME(3) NULL,
    version                  BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'optimistic lock version',
    created_at               DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at               DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_point_reservation_business_id (business_id),
    KEY idx_point_reservation_user_created (user_id, created_at),
    KEY idx_point_reservation_status_expires (status, expires_at),
    CONSTRAINT chk_point_reservation_amount CHECK (amount > 0),
    CONSTRAINT chk_point_reservation_status
        CHECK (status IN ('RESERVED', 'CONFIRMED', 'CANCELLED', 'EXPIRED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='point reservations';

-- Immutable transaction history used by point-service. business_id makes commands idempotent.
CREATE TABLE IF NOT EXISTS point_transactions (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_id    BIGINT UNSIGNED NOT NULL COMMENT 'idempotency key',
    user_id        BIGINT UNSIGNED NOT NULL,
    type           VARCHAR(16) NOT NULL COMMENT 'CREDIT or DEBIT',
    amount         BIGINT UNSIGNED NOT NULL,
    balance_before BIGINT UNSIGNED NOT NULL,
    balance_after  BIGINT UNSIGNED NOT NULL,
    source         VARCHAR(32) NOT NULL,
    remark         VARCHAR(200) NULL,
    created_at     DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_point_transaction_business_id (business_id),
    KEY idx_point_transaction_user_time (user_id, created_at),
    CONSTRAINT chk_point_transaction_type CHECK (type IN ('CREDIT', 'DEBIT')),
    CONSTRAINT chk_point_transaction_amount CHECK (amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='point transactions';
