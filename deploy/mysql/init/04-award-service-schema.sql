USE award_db;

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

CREATE TABLE IF NOT EXISTS award_issuances (
    id                     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '发奖记录ID',
    command_key            VARCHAR(64) NOT NULL COMMENT '发奖幂等号，例如LOTTERY:10001',
    source_type            ENUM('LOTTERY', 'REDEMPTION') NOT NULL COMMENT '发奖来源',
    source_record_id       BIGINT UNSIGNED NOT NULL COMMENT '抽奖记录ID或兑换记录ID',
    user_id                BIGINT UNSIGNED NOT NULL COMMENT '获奖用户ID',
    award_id               BIGINT UNSIGNED NOT NULL COMMENT '奖品ID',
    award_name_snapshot    VARCHAR(100) NOT NULL COMMENT '发奖时的奖品名称快照',
    award_type_snapshot    ENUM('VIRTUAL', 'POINTS') NOT NULL COMMENT '发奖时的奖品类型快照',
    award_payload_snapshot JSON NULL COMMENT '发奖参数快照',
    stock_no               BIGINT UNSIGNED NULL COMMENT '抽奖活动库存编号；兑换发奖为空',
    status                 ENUM('PROCESSING', 'SUCCEEDED', 'FAILED') NOT NULL DEFAULT 'PROCESSING' COMMENT '发奖执行状态',
    point_business_id      BIGINT UNSIGNED NULL COMMENT '积分发奖使用的稳定幂等业务号',
    result_ref             VARCHAR(128) NULL COMMENT '积分流水号或虚拟权益编号',
    retry_count            INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '执行重试次数',
    failure_code           VARCHAR(64) NULL COMMENT '最近失败错误码',
    last_error             VARCHAR(500) NULL COMMENT '最近失败原因',
    started_at             DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '首次开始发奖时间',
    succeeded_at           DATETIME(3) NULL COMMENT '发奖成功时间',
    created_at             DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at             DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_award_issuances_command_key (command_key),
    UNIQUE KEY uk_award_issuances_source (source_type, source_record_id),
    UNIQUE KEY uk_award_issuances_point_business (point_business_id),
    KEY idx_award_issuances_status_updated (status, updated_at),
    KEY idx_award_issuances_user_created (user_id, created_at),
    CONSTRAINT fk_award_issuances_award FOREIGN KEY (award_id) REFERENCES awards (id),
    CONSTRAINT chk_award_issuances_result CHECK (
        (status = 'SUCCEEDED' AND succeeded_at IS NOT NULL AND result_ref IS NOT NULL)
        OR status <> 'SUCCEEDED'
    ),
    CONSTRAINT chk_award_issuances_points_business CHECK (
        (award_type_snapshot = 'POINTS' AND point_business_id IS NOT NULL)
        OR (award_type_snapshot = 'VIRTUAL' AND point_business_id IS NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='幂等发奖执行记录';

CREATE TABLE IF NOT EXISTS user_awards (
    id                     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户奖品ID',
    user_id                BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    award_id               BIGINT UNSIGNED NOT NULL COMMENT '奖品ID',
    issuance_id            BIGINT UNSIGNED NOT NULL COMMENT '发奖记录ID',
    source_type            ENUM('LOTTERY', 'REDEMPTION') NOT NULL COMMENT '获得来源',
    source_record_id       BIGINT UNSIGNED NOT NULL COMMENT '来源记录ID',
    award_name_snapshot    VARCHAR(100) NOT NULL COMMENT '奖品名称快照',
    award_type_snapshot    ENUM('VIRTUAL', 'POINTS') NOT NULL COMMENT '奖品类型快照',
    award_payload_snapshot JSON NULL COMMENT '奖品参数快照',
    obtained_at            DATETIME(3) NOT NULL COMMENT '获得时间',
    created_at             DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_awards_issuance (issuance_id),
    KEY idx_user_awards_user_obtained (user_id, obtained_at),
    KEY idx_user_awards_award (award_id),
    CONSTRAINT fk_user_awards_award FOREIGN KEY (award_id) REFERENCES awards (id),
    CONSTRAINT fk_user_awards_issuance FOREIGN KEY (issuance_id) REFERENCES award_issuances (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='我的奖品测试记录';

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
