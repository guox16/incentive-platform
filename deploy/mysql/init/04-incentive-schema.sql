USE incentive_db;

-- 活动基础信息只保存各类活动共有的生命周期字段，具体规则由对应规则表维护。
CREATE TABLE IF NOT EXISTS incentive_activities (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '活动ID',
    code          VARCHAR(64) NOT NULL COMMENT '活动唯一编码',
    activity_type VARCHAR(32) NOT NULL COMMENT '活动类型：CHECK_IN签到、LOTTERY抽奖、REDEMPTION兑换',
    name          VARCHAR(100) NOT NULL COMMENT '活动名称',
    status        VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT '活动状态：DRAFT草稿、ACTIVE进行中、PAUSED暂停、ENDED结束',
    starts_at     DATETIME(3) NOT NULL COMMENT '活动开始时间',
    ends_at       DATETIME(3) NULL COMMENT '活动结束时间，为空表示长期有效',
    created_at    DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at    DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_incentive_activities_code (code),
    KEY idx_incentive_activities_type_status (activity_type, status),
    KEY idx_incentive_activities_active_time (starts_at, ends_at),
    CONSTRAINT chk_incentive_activities_type
        CHECK (activity_type IN ('CHECK_IN', 'LOTTERY', 'REDEMPTION')),
    CONSTRAINT chk_incentive_activities_status
        CHECK (status IN ('DRAFT', 'ACTIVE', 'PAUSED', 'ENDED')),
    CONSTRAINT chk_incentive_activities_time
        CHECK (ends_at IS NULL OR ends_at > starts_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='激励活动基础信息';

-- 签到规则按版本保存，规则调整时新增版本，历史签到仍可追溯到原规则。
CREATE TABLE IF NOT EXISTS check_in_rules (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '签到规则ID',
    activity_id    BIGINT UNSIGNED NOT NULL COMMENT '所属活动ID',
    rule_version   INT UNSIGNED NOT NULL COMMENT '规则版本号',
    timezone       VARCHAR(64) NOT NULL DEFAULT 'Asia/Shanghai' COMMENT '签到业务时区',
    cycle_days     INT UNSIGNED NOT NULL DEFAULT 7 COMMENT '连续奖励周期天数',
    base_points    BIGINT UNSIGNED NOT NULL COMMENT '未命中奖励档位时的基础积分',
    reset_on_miss  TINYINT(1) NOT NULL DEFAULT 1 COMMENT '漏签后是否重置连续天数：1是、0否',
    status         VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT '规则状态：DRAFT草稿、ACTIVE生效、RETIRED停用',
    effective_from DATETIME(3) NOT NULL COMMENT '规则生效时间',
    created_at     DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at     DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_check_in_rules_activity_version (activity_id, rule_version),
    KEY idx_check_in_rules_activity_status_time (activity_id, status, effective_from),
    CONSTRAINT fk_check_in_rules_activity
        FOREIGN KEY (activity_id) REFERENCES incentive_activities (id),
    CONSTRAINT chk_check_in_rules_version CHECK (rule_version > 0),
    CONSTRAINT chk_check_in_rules_cycle_days CHECK (cycle_days > 0),
    CONSTRAINT chk_check_in_rules_reset_on_miss CHECK (reset_on_miss IN (0, 1)),
    CONSTRAINT chk_check_in_rules_status CHECK (status IN ('DRAFT', 'ACTIVE', 'RETIRED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='版本化签到规则';

-- 某连续天数没有配置档位时使用签到规则中的基础积分。
CREATE TABLE IF NOT EXISTS check_in_reward_tiers (
    rule_id       BIGINT UNSIGNED NOT NULL COMMENT '签到规则ID',
    streak_day    INT UNSIGNED NOT NULL COMMENT '连续签到第几天',
    reward_points BIGINT UNSIGNED NOT NULL COMMENT '该签到日应发放的总积分',
    reward_label  VARCHAR(64) NULL COMMENT '奖励档位展示名称',
    created_at    DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (rule_id, streak_day),
    CONSTRAINT fk_check_in_reward_tiers_rule
        FOREIGN KEY (rule_id) REFERENCES check_in_rules (id),
    CONSTRAINT chk_check_in_reward_tiers_streak_day CHECK (streak_day > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='连续签到奖励档位';

-- 每条记录是一项不可重复的签到事实，连续天数和奖励是签到当时的计算快照。
CREATE TABLE IF NOT EXISTS daily_check_ins (
    id                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '签到记录ID',
    activity_id          BIGINT UNSIGNED NOT NULL COMMENT '签到活动ID',
    rule_id              BIGINT UNSIGNED NOT NULL COMMENT '本次签到采用的规则ID',
    rule_version         INT UNSIGNED NOT NULL COMMENT '本次签到采用的规则版本号',
    user_id              BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    check_in_date        DATE NOT NULL COMMENT '按签到规则时区计算的业务日期',
    streak_days          INT UNSIGNED NOT NULL COMMENT '包含本次签到在内的连续签到天数',
    reward_points        BIGINT UNSIGNED NOT NULL COMMENT '本次签到应发放的总积分',
    reward_status        VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '发奖状态：PENDING待发放、AWARDED已发放、FAILED发放失败',
    point_transaction_id BIGINT UNSIGNED NULL COMMENT '积分服务返回的积分流水ID',
    rewarded_at          DATETIME(3) NULL COMMENT '积分发放成功时间',
    created_at           DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at           DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_daily_check_ins_activity_user_date (activity_id, user_id, check_in_date),
    KEY idx_daily_check_ins_user_date (user_id, check_in_date DESC),
    KEY idx_daily_check_ins_reward_status (reward_status, created_at),
    CONSTRAINT fk_daily_check_ins_activity
        FOREIGN KEY (activity_id) REFERENCES incentive_activities (id),
    CONSTRAINT fk_daily_check_ins_rule
        FOREIGN KEY (rule_id) REFERENCES check_in_rules (id),
    CONSTRAINT chk_daily_check_ins_rule_version CHECK (rule_version > 0),
    CONSTRAINT chk_daily_check_ins_streak_days CHECK (streak_days > 0),
    CONSTRAINT chk_daily_check_ins_reward_status
        CHECK (reward_status IN ('PENDING', 'AWARDED', 'FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户每日签到记录';

-- 事务内写入待处理任务，由后台任务可靠调用积分服务或奖品服务。
CREATE TABLE IF NOT EXISTS incentive_outbox (
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '任务ID',
    event_type       VARCHAR(64) NOT NULL COMMENT '事件类型，例如CHECK_IN_POINTS_AWARD',
    aggregate_type   VARCHAR(32) NOT NULL COMMENT '业务对象类型，例如DAILY_CHECK_IN',
    aggregate_id     BIGINT UNSIGNED NOT NULL COMMENT '业务对象ID，同时作为下游幂等依据',
    payload          JSON NOT NULL COMMENT '调用下游服务所需的事件数据',
    status           VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '任务状态：PENDING待处理、PROCESSING处理中、PROCESSED已完成、FAILED已失败',
    retry_count      INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '已重试次数',
    next_retry_at    DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '下次允许重试时间',
    locked_at        DATETIME(3) NULL COMMENT '任务被处理器锁定的时间',
    last_error       VARCHAR(500) NULL COMMENT '最近一次处理失败原因',
    processed_at     DATETIME(3) NULL COMMENT '处理完成时间',
    created_at       DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at       DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_incentive_outbox_event_aggregate (event_type, aggregate_type, aggregate_id),
    KEY idx_incentive_outbox_dispatch (status, next_retry_at, id),
    CONSTRAINT chk_incentive_outbox_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'PROCESSED', 'FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='激励业务可靠事件任务';
