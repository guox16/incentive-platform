USE incentive_db;

CREATE TABLE IF NOT EXISTS lottery_pre_draw_rules (
    id                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '抽奖前置规则配置ID',
    activity_id           BIGINT UNSIGNED NOT NULL COMMENT '所属抽奖活动ID',
    participation_rule_id BIGINT UNSIGNED NOT NULL COMMENT '所属参与规则版本ID',
    rule_type             VARCHAR(64) NOT NULL COMMENT '规则节点类型',
    execution_order       INT UNSIGNED NOT NULL COMMENT '责任链执行顺序',
    enabled               TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    rule_config           JSON NOT NULL COMMENT '节点配置：名单数组、解锁Map、积分档位Map或幸运奖ID',
    created_at            DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at            DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_lottery_pre_draw_rules_type (participation_rule_id, rule_type),
    KEY idx_lottery_pre_draw_rules_activity_version (activity_id, participation_rule_id, execution_order),
    CONSTRAINT fk_lottery_pre_draw_rules_activity
        FOREIGN KEY (activity_id) REFERENCES incentive_activities (id),
    CONSTRAINT fk_lottery_pre_draw_rules_participation
        FOREIGN KEY (participation_rule_id) REFERENCES activity_participation_rules (id),
    CONSTRAINT chk_lottery_pre_draw_rules_enabled CHECK (enabled IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='抽奖前置责任链规则配置';

ALTER TABLE activity_participation_rules DROP COLUMN qualification_rule;
ALTER TABLE lottery_prizes DROP COLUMN eligibility_rule;
