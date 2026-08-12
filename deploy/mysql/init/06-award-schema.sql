USE award_db;

CREATE TABLE IF NOT EXISTS prizes (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code            VARCHAR(64) NOT NULL,
    name            VARCHAR(100) NOT NULL,
    prize_type      VARCHAR(16) NOT NULL,
    status          VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    available_stock BIGINT NOT NULL DEFAULT 0,
    award_payload   JSON NULL,
    version         BIGINT UNSIGNED NOT NULL DEFAULT 0,
    deleted_at      DATETIME(3) NULL,
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_prizes_code (code),
    KEY idx_prizes_status_deleted (status, deleted_at),
    CONSTRAINT chk_prizes_type CHECK (prize_type IN ('VIRTUAL', 'POINTS', 'NONE')),
    CONSTRAINT chk_prizes_status CHECK (status IN ('DRAFT', 'ACTIVE', 'INACTIVE', 'DELETED')),
    CONSTRAINT chk_prizes_stock CHECK (available_stock >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='奖品主数据';

CREATE TABLE IF NOT EXISTS prize_inventory_ledgers (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    prize_id       BIGINT UNSIGNED NOT NULL,
    business_no    VARCHAR(64) NOT NULL,
    operation_type VARCHAR(32) NOT NULL,
    change_amount  BIGINT NOT NULL,
    balance_after  BIGINT NOT NULL,
    remark         VARCHAR(256) NULL,
    created_at     DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_prize_inventory_ledger_business_no (business_no),
    KEY idx_prize_inventory_ledger_prize_created (prize_id, created_at),
    CONSTRAINT fk_prize_inventory_ledger_prize FOREIGN KEY (prize_id) REFERENCES prizes (id),
    CONSTRAINT chk_prize_inventory_ledger_balance CHECK (balance_after >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='奖品库存流水';
