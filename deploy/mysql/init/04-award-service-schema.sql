USE award_db;

CREATE TABLE IF NOT EXISTS prizes (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code            VARCHAR(64) NOT NULL,
    name            VARCHAR(100) NOT NULL,
    prize_type      ENUM('VIRTUAL', 'POINTS', 'NONE') NOT NULL,
    status          ENUM('DRAFT', 'ACTIVE', 'INACTIVE', 'DELETED') NOT NULL DEFAULT 'DRAFT',
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

CREATE TABLE IF NOT EXISTS awards (
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '奖品ID',
    code              VARCHAR(64) NOT NULL COMMENT '奖品唯一编码',
    name              VARCHAR(100) NOT NULL COMMENT '奖品名称',
    award_type        ENUM('VIRTUAL', 'POINTS', 'NONE') NOT NULL COMMENT '奖品类型：VIRTUAL虚拟权益、POINTS积分、NONE谢谢参与',
    status            ENUM('ACTIVE', 'INACTIVE', 'DELETED') NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE上架、INACTIVE下架、DELETED软删除',
    cover_url         VARCHAR(500) NULL COMMENT '奖品封面地址',
    award_payload     JSON NULL COMMENT '未来发奖所需参数',
    total_stock       BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '真实库存总量',
    available_stock   BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '真实可用库存，一期只维护不扣减',
    version           BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at        DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at        DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_awards_code (code),
    KEY idx_awards_status_type (status, award_type),
    CONSTRAINT chk_awards_type CHECK (award_type IN ('VIRTUAL', 'POINTS', 'NONE')),
    CONSTRAINT chk_awards_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED')),
    CONSTRAINT chk_awards_stock CHECK (available_stock <= total_stock)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='奖品主数据与真实库存';

CREATE TABLE IF NOT EXISTS award_inventory_ledger (
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '库存流水ID',
    award_id         BIGINT UNSIGNED NOT NULL COMMENT '奖品ID',
    business_no      VARCHAR(64) NOT NULL COMMENT '库存业务幂等号',
    operation_type   VARCHAR(16) NOT NULL COMMENT '操作类型：INIT、INCREASE、DECREASE、RESERVE、RELEASE',
    change_amount    BIGINT NOT NULL COMMENT '可用库存变动量',
    available_after  BIGINT UNSIGNED NOT NULL COMMENT '变动后可用库存',
    remark           VARCHAR(256) NULL COMMENT '备注',
    created_at       DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_award_inventory_business_no (business_no),
    KEY idx_award_inventory_award_created (award_id, created_at),
    CONSTRAINT fk_award_inventory_award FOREIGN KEY (award_id) REFERENCES awards (id),
    CONSTRAINT chk_award_inventory_operation
        CHECK (operation_type IN ('INIT', 'INCREASE', 'DECREASE', 'RESERVE', 'RELEASE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='奖品真实库存流水预留模型';

INSERT IGNORE INTO awards
    (code, name, award_type, status, cover_url, award_payload, total_stock, available_stock)
VALUES
    ('WELCOME_COUPON', '新人优惠券', 'VIRTUAL', 'ACTIVE', NULL,
     JSON_OBJECT('templateCode', 'WELCOME_10'), 1000, 1000),
    ('BONUS_POINTS_100', '100积分', 'POINTS', 'ACTIVE', NULL,
     JSON_OBJECT('points', 100), 1000, 1000),
    ('THANKS', '谢谢参与', 'NONE', 'ACTIVE', NULL, NULL, 0, 0);
