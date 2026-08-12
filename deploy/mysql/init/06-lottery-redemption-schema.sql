USE award_db;

CREATE TABLE IF NOT EXISTS awards (
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '奖品ID',
    code              VARCHAR(64) NOT NULL COMMENT '奖品唯一编码',
    name              VARCHAR(100) NOT NULL COMMENT '奖品名称',
    award_type        VARCHAR(16) NOT NULL COMMENT '奖品类型：VIRTUAL虚拟权益、POINTS积分、NONE谢谢参与',
    status            VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE上架、INACTIVE下架、DELETED软删除',
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

USE incentive_db;

-- 抽奖和兑换共享规则版本及资格扩展字段，具体商品价格仍保存在兑换商品上。
CREATE TABLE IF NOT EXISTS activity_participation_rules (
    id                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '参与规则ID',
    activity_id        BIGINT UNSIGNED NOT NULL COMMENT '活动ID',
    rule_version       INT UNSIGNED NOT NULL COMMENT '规则版本号',
    points_cost        BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '抽奖单次积分成本；兑换活动填0',
    daily_limit        INT UNSIGNED NULL COMMENT '每用户每日最大参与次数，为空表示不限',
    qualification_rule JSON NULL COMMENT '黑白名单、人群、时段等资格规则扩展',
    status             VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT草稿、ACTIVE生效、RETIRED停用',
    effective_from     DATETIME(3) NOT NULL COMMENT '生效时间',
    created_at         DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at         DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_participation_rules_activity_version (activity_id, rule_version),
    KEY idx_participation_rules_activity_status (activity_id, status, effective_from),
    CONSTRAINT fk_participation_rules_activity
        FOREIGN KEY (activity_id) REFERENCES incentive_activities (id),
    CONSTRAINT chk_participation_rules_version CHECK (rule_version > 0),
    CONSTRAINT chk_participation_rules_daily_limit CHECK (daily_limit IS NULL OR daily_limit > 0),
    CONSTRAINT chk_participation_rules_status CHECK (status IN ('DRAFT', 'ACTIVE', 'RETIRED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='抽奖兑换版本化参与规则';

CREATE TABLE IF NOT EXISTS lottery_prizes (
    id                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '活动奖品配置ID',
    activity_id           BIGINT UNSIGNED NOT NULL COMMENT '抽奖活动ID',
    rule_id               BIGINT UNSIGNED NOT NULL COMMENT '参与规则ID',
    prize_id              BIGINT UNSIGNED NOT NULL COMMENT 'award-service奖品ID，不建跨库外键',
    prize_name_snapshot   VARCHAR(100) NOT NULL COMMENT '奖品名称快照',
    prize_type_snapshot   VARCHAR(16) NOT NULL COMMENT '奖品类型快照',
    cover_url_snapshot    VARCHAR(500) NULL COMMENT '奖品封面快照',
    award_payload_snapshot JSON NULL COMMENT '发奖参数快照',
    weight                BIGINT UNSIGNED NOT NULL COMMENT '抽奖权重，发布时约分后写入Redis槽位池',
    campaign_quota        BIGINT UNSIGNED NULL COMMENT '活动投放名额，为空表示不限；一期不扣减',
    display_order         INT NOT NULL DEFAULT 0 COMMENT '展示顺序',
    eligibility_rule     JSON NULL COMMENT '奖品解锁规则扩展',
    created_at            DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at            DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_lottery_prizes_rule_prize (rule_id, prize_id),
    KEY idx_lottery_prizes_activity_order (activity_id, display_order, id),
    CONSTRAINT fk_lottery_prizes_activity FOREIGN KEY (activity_id) REFERENCES incentive_activities (id),
    CONSTRAINT fk_lottery_prizes_rule FOREIGN KEY (rule_id) REFERENCES activity_participation_rules (id),
    CONSTRAINT chk_lottery_prizes_type CHECK (prize_type_snapshot IN ('VIRTUAL', 'POINTS', 'NONE')),
    CONSTRAINT chk_lottery_prizes_weight CHECK (weight > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='抽奖活动奖品与概率配置';

CREATE TABLE IF NOT EXISTS redemption_items (
    id                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '兑换商品ID',
    activity_id           BIGINT UNSIGNED NOT NULL COMMENT '兑换活动ID',
    rule_id               BIGINT UNSIGNED NOT NULL COMMENT '参与规则ID',
    item_code             VARCHAR(64) NOT NULL COMMENT '活动内兑换商品编码',
    prize_id              BIGINT UNSIGNED NOT NULL COMMENT 'award-service奖品ID，不建跨库外键',
    prize_name_snapshot   VARCHAR(100) NOT NULL COMMENT '奖品名称快照',
    prize_type_snapshot   VARCHAR(16) NOT NULL COMMENT '奖品类型快照',
    cover_url_snapshot    VARCHAR(500) NULL COMMENT '奖品封面快照',
    award_payload_snapshot JSON NULL COMMENT '发奖参数快照',
    points_price          BIGINT UNSIGNED NOT NULL COMMENT '兑换积分价格',
    campaign_quota        BIGINT UNSIGNED NULL COMMENT '活动投放名额，为空表示不限；一期不扣减',
    display_order         INT NOT NULL DEFAULT 0 COMMENT '展示顺序',
    eligibility_rule     JSON NULL COMMENT '商品差异化资格规则扩展',
    status                VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE可兑换、INACTIVE下架',
    created_at            DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at            DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_redemption_items_activity_code (activity_id, item_code),
    KEY idx_redemption_items_activity_status_order (activity_id, status, display_order, id),
    CONSTRAINT fk_redemption_items_activity FOREIGN KEY (activity_id) REFERENCES incentive_activities (id),
    CONSTRAINT fk_redemption_items_rule FOREIGN KEY (rule_id) REFERENCES activity_participation_rules (id),
    CONSTRAINT chk_redemption_items_type CHECK (prize_type_snapshot IN ('VIRTUAL', 'POINTS')),
    CONSTRAINT chk_redemption_items_price CHECK (points_price > 0),
    CONSTRAINT chk_redemption_items_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='兑换活动商品配置';

CREATE TABLE IF NOT EXISTS lottery_participations (
    id                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '抽奖参与记录ID',
    activity_id           BIGINT UNSIGNED NOT NULL COMMENT '活动ID',
    rule_id               BIGINT UNSIGNED NOT NULL COMMENT '规则ID',
    rule_version          INT UNSIGNED NOT NULL COMMENT '规则版本快照',
    user_id               BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    lottery_prize_id      BIGINT UNSIGNED NOT NULL COMMENT '命中的活动奖品配置ID',
    prize_id              BIGINT UNSIGNED NOT NULL COMMENT '奖品主数据ID快照',
    prize_name_snapshot   VARCHAR(100) NOT NULL COMMENT '奖品名称快照',
    prize_type_snapshot   VARCHAR(16) NOT NULL COMMENT '奖品类型快照',
    cover_url_snapshot    VARCHAR(500) NULL COMMENT '奖品封面快照',
    award_payload_snapshot JSON NULL COMMENT '发奖参数快照',
    points_cost           BIGINT UNSIGNED NOT NULL COMMENT '本次扣除积分',
    eligibility_result    JSON NULL COMMENT '资格校验结果快照',
    point_transaction_id  BIGINT UNSIGNED NOT NULL COMMENT '积分服务扣减流水ID',
    created_at            DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '参与时间',
    PRIMARY KEY (id),
    KEY idx_lottery_participations_user_time (user_id, created_at),
    KEY idx_lottery_participations_activity_user_time (activity_id, user_id, created_at),
    CONSTRAINT fk_lottery_participations_activity FOREIGN KEY (activity_id) REFERENCES incentive_activities (id),
    CONSTRAINT fk_lottery_participations_rule FOREIGN KEY (rule_id) REFERENCES activity_participation_rules (id),
    CONSTRAINT fk_lottery_participations_prize FOREIGN KEY (lottery_prize_id) REFERENCES lottery_prizes (id),
    CONSTRAINT chk_lottery_participations_type CHECK (prize_type_snapshot IN ('VIRTUAL', 'POINTS', 'NONE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='抽奖参与结果与快照';

CREATE TABLE IF NOT EXISTS redemption_records (
    id                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '兑换记录ID',
    activity_id           BIGINT UNSIGNED NOT NULL COMMENT '活动ID',
    rule_id               BIGINT UNSIGNED NOT NULL COMMENT '规则ID',
    rule_version          INT UNSIGNED NOT NULL COMMENT '规则版本快照',
    item_id               BIGINT UNSIGNED NOT NULL COMMENT '兑换商品ID',
    item_code_snapshot    VARCHAR(64) NOT NULL COMMENT '商品编码快照',
    user_id               BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    prize_id              BIGINT UNSIGNED NOT NULL COMMENT '奖品主数据ID快照',
    prize_name_snapshot   VARCHAR(100) NOT NULL COMMENT '奖品名称快照',
    prize_type_snapshot   VARCHAR(16) NOT NULL COMMENT '奖品类型快照',
    cover_url_snapshot    VARCHAR(500) NULL COMMENT '奖品封面快照',
    award_payload_snapshot JSON NULL COMMENT '发奖参数快照',
    points_cost           BIGINT UNSIGNED NOT NULL COMMENT '本次扣除积分',
    eligibility_result    JSON NULL COMMENT '资格校验结果快照',
    point_transaction_id  BIGINT UNSIGNED NOT NULL COMMENT '积分服务扣减流水ID',
    created_at            DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '兑换时间',
    PRIMARY KEY (id),
    KEY idx_redemption_records_user_time (user_id, created_at),
    KEY idx_redemption_records_activity_user_time (activity_id, user_id, created_at),
    CONSTRAINT fk_redemption_records_activity FOREIGN KEY (activity_id) REFERENCES incentive_activities (id),
    CONSTRAINT fk_redemption_records_rule FOREIGN KEY (rule_id) REFERENCES activity_participation_rules (id),
    CONSTRAINT fk_redemption_records_item FOREIGN KEY (item_id) REFERENCES redemption_items (id),
    CONSTRAINT chk_redemption_records_type CHECK (prize_type_snapshot IN ('VIRTUAL', 'POINTS'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='兑换记录与奖品快照';

CREATE TABLE IF NOT EXISTS pending_awards (
    id                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '待发奖任务ID',
    source_type           VARCHAR(16) NOT NULL COMMENT '来源：LOTTERY抽奖、REDEMPTION兑换',
    source_record_id      BIGINT UNSIGNED NOT NULL COMMENT '来源参与记录ID',
    user_id               BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    prize_id              BIGINT UNSIGNED NOT NULL COMMENT '奖品主数据ID快照',
    prize_name_snapshot   VARCHAR(100) NOT NULL COMMENT '奖品名称快照',
    prize_type_snapshot   VARCHAR(16) NOT NULL COMMENT '奖品类型快照',
    award_payload_snapshot JSON NULL COMMENT '发奖参数快照',
    status                VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING待发放、PROCESSING发放中、AWARDED已发放、FAILED失败',
    retry_count           INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '重试次数预留',
    last_error            VARCHAR(500) NULL COMMENT '最近错误',
    awarded_at            DATETIME(3) NULL COMMENT '发放成功时间',
    created_at            DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at            DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_pending_awards_source (source_type, source_record_id),
    KEY idx_pending_awards_status_created (status, created_at),
    KEY idx_pending_awards_user_created (user_id, created_at),
    CONSTRAINT chk_pending_awards_source CHECK (source_type IN ('LOTTERY', 'REDEMPTION')),
    CONSTRAINT chk_pending_awards_type CHECK (prize_type_snapshot IN ('VIRTUAL', 'POINTS')),
    CONSTRAINT chk_pending_awards_status CHECK (status IN ('PENDING', 'PROCESSING', 'AWARDED', 'FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='抽奖兑换待发奖记录';

-- 演示活动由初始化脚本配置；业务接口只读取进行中的活动。
INSERT IGNORE INTO incentive_activities
    (code, activity_type, name, status, starts_at, ends_at)
VALUES
    ('SUMMER_LOTTERY', 'LOTTERY', '夏日幸运抽奖', 'ACTIVE', '2025-01-01 00:00:00.000', '2035-01-01 00:00:00.000'),
    ('POINTS_MALL', 'REDEMPTION', '积分兑换专区', 'ACTIVE', '2025-01-01 00:00:00.000', '2035-01-01 00:00:00.000');

INSERT IGNORE INTO activity_participation_rules
    (activity_id, rule_version, points_cost, daily_limit, qualification_rule, status, effective_from)
SELECT id, 1, 10, 3, JSON_OBJECT(), 'ACTIVE', '2025-01-01 00:00:00.000'
FROM incentive_activities WHERE code = 'SUMMER_LOTTERY';

INSERT IGNORE INTO activity_participation_rules
    (activity_id, rule_version, points_cost, daily_limit, qualification_rule, status, effective_from)
SELECT id, 1, 0, 5, JSON_OBJECT(), 'ACTIVE', '2025-01-01 00:00:00.000'
FROM incentive_activities WHERE code = 'POINTS_MALL';

INSERT IGNORE INTO lottery_prizes
    (activity_id, rule_id, prize_id, prize_name_snapshot, prize_type_snapshot,
     cover_url_snapshot, award_payload_snapshot, weight, campaign_quota, display_order, eligibility_rule)
SELECT activity.id, rule_config.id, award.id, award.name, award.award_type,
       award.cover_url, award.award_payload,
       CASE award.code WHEN 'WELCOME_COUPON' THEN 1 WHEN 'BONUS_POINTS_100' THEN 3 ELSE 6 END,
       NULL,
       CASE award.code WHEN 'WELCOME_COUPON' THEN 1 WHEN 'BONUS_POINTS_100' THEN 2 ELSE 3 END,
       JSON_OBJECT()
FROM incentive_activities activity
JOIN activity_participation_rules rule_config ON rule_config.activity_id = activity.id
JOIN award_db.awards award ON award.code IN ('WELCOME_COUPON', 'BONUS_POINTS_100', 'THANKS')
WHERE activity.code = 'SUMMER_LOTTERY' AND rule_config.rule_version = 1;

INSERT IGNORE INTO redemption_items
    (activity_id, rule_id, item_code, prize_id, prize_name_snapshot, prize_type_snapshot,
     cover_url_snapshot, award_payload_snapshot, points_price, campaign_quota, display_order,
     eligibility_rule, status)
SELECT activity.id, rule_config.id, award.code, award.id, award.name, award.award_type,
       award.cover_url, award.award_payload,
       CASE award.code WHEN 'WELCOME_COUPON' THEN 50 ELSE 100 END,
       NULL,
       CASE award.code WHEN 'WELCOME_COUPON' THEN 1 ELSE 2 END,
       JSON_OBJECT(), 'ACTIVE'
FROM incentive_activities activity
JOIN activity_participation_rules rule_config ON rule_config.activity_id = activity.id
JOIN award_db.awards award ON award.code IN ('WELCOME_COUPON', 'BONUS_POINTS_100')
WHERE activity.code = 'POINTS_MALL' AND rule_config.rule_version = 1;
