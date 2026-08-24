-- 本地环境全流程测试数据
-- 管理员账号：modeladmin / 123456
-- 普通用户账号：modeluser / 123456
-- 名单规则用户：blackuser / 123456
-- 警告：执行本脚本会清空四个业务数据库中的全部现有数据，再写入初始化数据。
-- Redis库存编号不属于MySQL；重复初始化时请同时清空Redis测试库。

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------------
-- 清空全部业务数据并重置自增主键
-- TRUNCATE 会隐式提交，整个初始化过程不具备事务回滚能力。
-- ---------------------------------------------------------------------------
SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE incentive_db.pending_awards;
TRUNCATE TABLE incentive_db.redemption_records;
TRUNCATE TABLE incentive_db.lottery_records;
TRUNCATE TABLE incentive_db.lottery_orders;
TRUNCATE TABLE incentive_db.redemption_items;
TRUNCATE TABLE incentive_db.lottery_prizes;
TRUNCATE TABLE incentive_db.lottery_pre_draw_rules;
TRUNCATE TABLE incentive_db.daily_check_ins;
TRUNCATE TABLE incentive_db.check_in_reward_tiers;
TRUNCATE TABLE incentive_db.check_in_rules;
TRUNCATE TABLE incentive_db.activity_participation_rules;
TRUNCATE TABLE incentive_db.incentive_activities;

TRUNCATE TABLE award_db.award_inventory_ledger;
TRUNCATE TABLE award_db.awards;
TRUNCATE TABLE award_db.prize_inventory_ledgers;
TRUNCATE TABLE award_db.prizes;

TRUNCATE TABLE points_db.point_reservations;
TRUNCATE TABLE points_db.point_transactions;
TRUNCATE TABLE points_db.point_accounts;

TRUNCATE TABLE user_db.membership_records;
TRUNCATE TABLE user_db.user_memberships;
TRUNCATE TABLE user_db.refresh_tokens;
TRUNCATE TABLE user_db.user_roles;
TRUNCATE TABLE user_db.users;

SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------------------
-- user_db：管理员、普通业务用户和名单规则用户
-- ---------------------------------------------------------------------------
USE user_db;

INSERT INTO users (username, password_hash, nickname, mobile, status)
VALUES
    ('modeladmin',
     '$2a$10$4MYPbxVfkO8.b/kOoWVT8.YtAGoI3s7fSCNbkQt3jjZ59vK.htAIy',
     '超级管理员', '13800000001', 1),
    ('modeluser',
     '$2a$10$4MYPbxVfkO8.b/kOoWVT8.YtAGoI3s7fSCNbkQt3jjZ59vK.htAIy',
     '流程测试用户', '13800000002', 1),
    ('blackuser',
     '$2a$10$4MYPbxVfkO8.b/kOoWVT8.YtAGoI3s7fSCNbkQt3jjZ59vK.htAIy',
     '名单规则用户', '13800000003', 1)
ON DUPLICATE KEY UPDATE
    password_hash = VALUES(password_hash),
    nickname = VALUES(nickname),
    mobile = VALUES(mobile),
    status = VALUES(status);

SET @debug_user_id = (
    SELECT id FROM users WHERE username = 'modeladmin' LIMIT 1
);
SET @model_user_id = (
    SELECT id FROM users WHERE username = 'modeluser' LIMIT 1
);
SET @black_user_id = (
    SELECT id FROM users WHERE username = 'blackuser' LIMIT 1
);

INSERT INTO user_roles (user_id, role)
VALUES
    (@debug_user_id, 'SUPER_ADMIN'),
    (@model_user_id, 'USER'),
    (@black_user_id, 'USER')
ON DUPLICATE KEY UPDATE role = VALUES(role);

INSERT INTO user_memberships (user_id, expires_at)
VALUES
    (@debug_user_id, '2035-01-01 00:00:00.000'),
    (@model_user_id, '2035-01-01 00:00:00.000'),
    (@black_user_id, '2035-01-01 00:00:00.000')
ON DUPLICATE KEY UPDATE expires_at = VALUES(expires_at);

-- ---------------------------------------------------------------------------
-- points_db：三个测试用户的积分账户及初始化流水
-- ---------------------------------------------------------------------------
USE points_db;

INSERT INTO point_accounts (user_id, balance, version)
VALUES
    (@debug_user_id, 1000, 0),
    (@model_user_id, 1000, 0),
    (@black_user_id, 1000, 0)
ON DUPLICATE KEY UPDATE
    balance = VALUES(balance),
    version = VALUES(version);

INSERT INTO point_transactions
    (business_id, user_id, type, amount, balance_before, balance_after, source, remark)
VALUES
    (900000000000000001, @debug_user_id, 'CREDIT', 1000, 0, 1000,
     'DEBUG_INIT', '管理员初始化积分'),
    (900000000000000002, @model_user_id, 'CREDIT', 1000, 0, 1000,
     'DEBUG_INIT', '普通用户初始化积分'),
    (900000000000000003, @black_user_id, 'CREDIT', 1000, 0, 1000,
     'DEBUG_INIT', '名单用户初始化积分')
ON DUPLICATE KEY UPDATE
    user_id = VALUES(user_id),
    type = VALUES(type),
    amount = VALUES(amount),
    balance_before = VALUES(balance_before),
    balance_after = VALUES(balance_after),
    source = VALUES(source),
    remark = VALUES(remark);

-- ---------------------------------------------------------------------------
-- award_db：三个可用于抽奖的奖品
-- ---------------------------------------------------------------------------
USE award_db;

INSERT INTO awards
    (code, name, award_type, status, cover_url, award_payload, total_stock, available_stock)
VALUES
    ('DEBUG_COUPON_20', '20元调试优惠券', 'VIRTUAL', 'ACTIVE', NULL,
     JSON_OBJECT('templateCode', 'DEBUG_COUPON_20', 'amount', 20), 100, 100),
    ('DEBUG_POINTS_100', '100调试积分', 'POINTS', 'ACTIVE', NULL,
     JSON_OBJECT('points', 100), 500, 500),
    ('DEBUG_THANKS', '谢谢参与', 'NONE', 'ACTIVE', NULL,
     NULL, 0, 0)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    award_type = VALUES(award_type),
    status = VALUES(status),
    cover_url = VALUES(cover_url),
    award_payload = VALUES(award_payload),
    total_stock = VALUES(total_stock),
    available_stock = VALUES(available_stock);

-- 管理端奖品页面当前使用prizes模型，写入同一组可管理奖品。
INSERT INTO prizes
    (code, name, prize_type, status, available_stock, award_payload, version)
VALUES
    ('DEBUG_COUPON_20', '20元调试优惠券', 'VIRTUAL', 'ACTIVE', 100,
     JSON_OBJECT('templateCode', 'DEBUG_COUPON_20', 'amount', 20), 0),
    ('DEBUG_POINTS_100', '100调试积分', 'POINTS', 'ACTIVE', 500,
     JSON_OBJECT('points', 100), 0),
    ('DEBUG_THANKS', '谢谢参与', 'NONE', 'ACTIVE', 0, NULL, 0)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    prize_type = VALUES(prize_type),
    status = VALUES(status),
    available_stock = VALUES(available_stock),
    award_payload = VALUES(award_payload),
    version = VALUES(version);

-- ---------------------------------------------------------------------------
-- incentive_db：一个有效抽奖活动、一版规则、三个活动奖品
-- 权重为 1:3:6，即理论概率分别为 10%、30%、60%。
-- ---------------------------------------------------------------------------
USE incentive_db;

INSERT INTO incentive_activities
    (code, activity_type, name, status, starts_at, ends_at)
VALUES
    ('DEBUG_LOTTERY', 'LOTTERY', '本地调试幸运抽奖', 'ACTIVE',
     '2025-01-01 00:00:00.000', '2035-01-01 00:00:00.000')
ON DUPLICATE KEY UPDATE
    activity_type = VALUES(activity_type),
    name = VALUES(name),
    status = VALUES(status),
    starts_at = VALUES(starts_at),
    ends_at = VALUES(ends_at);

SET @debug_activity_id = (
    SELECT id FROM incentive_activities WHERE code = 'DEBUG_LOTTERY' LIMIT 1
);

INSERT INTO activity_participation_rules
    (activity_id, rule_version, points_cost, daily_limit, status, effective_from)
VALUES
    (@debug_activity_id, 1, 10, 100, 'ACTIVE', '2025-01-01 00:00:00.000')
ON DUPLICATE KEY UPDATE
    points_cost = VALUES(points_cost),
    daily_limit = VALUES(daily_limit),
    status = VALUES(status),
    effective_from = VALUES(effective_from);

SET @debug_rule_id = (
    SELECT id
    FROM activity_participation_rules
    WHERE activity_id = @debug_activity_id AND rule_version = 1
    LIMIT 1
);

INSERT INTO lottery_prizes
    (activity_id, rule_id, prize_id, prize_name_snapshot, prize_type_snapshot,
     cover_url_snapshot, award_payload_snapshot, weight, campaign_quota,
     display_order)
SELECT
    @debug_activity_id,
    @debug_rule_id,
    award.id,
    award.name,
    award.award_type,
    award.cover_url,
    award.award_payload,
    CASE award.code
        WHEN 'DEBUG_COUPON_20' THEN 1
        WHEN 'DEBUG_POINTS_100' THEN 3
        ELSE 6
    END,
    CASE WHEN award.award_type = 'NONE' THEN NULL ELSE award.available_stock END,
    CASE award.code
        WHEN 'DEBUG_COUPON_20' THEN 1
        WHEN 'DEBUG_POINTS_100' THEN 2
        ELSE 3
    END
FROM award_db.awards AS award
WHERE award.code IN ('DEBUG_COUPON_20', 'DEBUG_POINTS_100', 'DEBUG_THANKS')
ON DUPLICATE KEY UPDATE
    activity_id = VALUES(activity_id),
    prize_name_snapshot = VALUES(prize_name_snapshot),
    prize_type_snapshot = VALUES(prize_type_snapshot),
    cover_url_snapshot = VALUES(cover_url_snapshot),
    award_payload_snapshot = VALUES(award_payload_snapshot),
    weight = VALUES(weight),
    campaign_quota = VALUES(campaign_quota),
    display_order = VALUES(display_order);

-- 快速核对本脚本生成的数据。
SELECT @debug_user_id AS debug_user_id,
       @debug_activity_id AS debug_activity_id,
       @debug_rule_id AS debug_rule_id;

SELECT activity.code AS activity_code,
       prize.prize_name_snapshot AS prize_name,
       prize.prize_type_snapshot AS prize_type,
       prize.weight,
       prize.display_order
FROM incentive_activities AS activity
JOIN lottery_prizes AS prize ON prize.activity_id = activity.id
WHERE activity.code = 'DEBUG_LOTTERY'
ORDER BY prize.display_order;

-- ---------------------------------------------------------------------------
-- 从 init 目录迁移出的演示奖品、抽奖活动和积分兑换活动
-- ---------------------------------------------------------------------------
USE award_db;

INSERT IGNORE INTO awards
    (code, name, award_type, status, cover_url, award_payload, total_stock, available_stock)
VALUES
    ('WELCOME_COUPON', '新人优惠券', 'VIRTUAL', 'ACTIVE', NULL,
     JSON_OBJECT('templateCode', 'WELCOME_10'), 1000, 1000),
    ('BONUS_POINTS_100', '100积分', 'POINTS', 'ACTIVE', NULL,
     JSON_OBJECT('points', 100), 1000, 1000),
    ('THANKS', '谢谢参与', 'NONE', 'ACTIVE', NULL, NULL, 0, 0),
    ('COFFEE_COUPON', '精品咖啡兑换券', 'VIRTUAL', 'ACTIVE', NULL,
     JSON_OBJECT('templateCode', 'COFFEE_FREE', 'validDays', 30), 200, 200),
    ('VIDEO_VIP_30D', '视频会员月卡', 'VIRTUAL', 'ACTIVE', NULL,
     JSON_OBJECT('membershipType', 'VIDEO_VIP', 'days', 30), 100, 100),
    ('SHOPPING_CARD_50', '50元购物卡', 'VIRTUAL', 'ACTIVE', NULL,
     JSON_OBJECT('cardType', 'SHOPPING_CARD', 'amount', 50), 50, 50);

INSERT IGNORE INTO prizes
    (code, name, prize_type, status, available_stock, award_payload, version)
VALUES
    ('WELCOME_COUPON', '新人优惠券', 'VIRTUAL', 'ACTIVE', 1000,
     JSON_OBJECT('templateCode', 'WELCOME_10'), 0),
    ('BONUS_POINTS_100', '100积分', 'POINTS', 'ACTIVE', 1000,
     JSON_OBJECT('points', 100), 0),
    ('THANKS', '谢谢参与', 'NONE', 'ACTIVE', 0, NULL, 0),
    ('COFFEE_COUPON', '精品咖啡兑换券', 'VIRTUAL', 'ACTIVE', 200,
     JSON_OBJECT('templateCode', 'COFFEE_FREE', 'validDays', 30), 0),
    ('VIDEO_VIP_30D', '视频会员月卡', 'VIRTUAL', 'ACTIVE', 100,
     JSON_OBJECT('membershipType', 'VIDEO_VIP', 'days', 30), 0),
    ('SHOPPING_CARD_50', '50元购物卡', 'VIRTUAL', 'ACTIVE', 50,
     JSON_OBJECT('cardType', 'SHOPPING_CARD', 'amount', 50), 0);

USE incentive_db;

INSERT IGNORE INTO incentive_activities
    (code, activity_type, name, status, starts_at, ends_at)
VALUES
    ('SUMMER_LOTTERY', 'LOTTERY', '夏日幸运抽奖', 'ACTIVE',
     '2025-01-01 00:00:00.000', '2035-01-01 00:00:00.000'),
    ('POINTS_MALL', 'REDEMPTION', '积分兑换专区', 'ACTIVE',
     '2025-01-01 00:00:00.000', '2035-01-01 00:00:00.000'),
    ('DAILY_CHECK_IN', 'CHECK_IN', '每日签到', 'ACTIVE',
     '2025-01-01 00:00:00.000', '2035-01-01 00:00:00.000');

INSERT IGNORE INTO check_in_rules
    (activity_id, rule_version, timezone, cycle_days, base_points,
     reset_on_miss, status, effective_from)
SELECT id, 1, 'Asia/Shanghai', 7, 10, 1, 'ACTIVE', '2025-01-01 00:00:00.000'
FROM incentive_activities
WHERE code = 'DAILY_CHECK_IN';

INSERT IGNORE INTO check_in_reward_tiers
    (rule_id, streak_day, reward_points, reward_label)
SELECT rule_config.id, tier.streak_day, tier.reward_points, tier.reward_label
FROM check_in_rules AS rule_config
JOIN incentive_activities AS activity ON activity.id = rule_config.activity_id
JOIN (
    SELECT 1 AS streak_day, 10 AS reward_points, '第1天' AS reward_label
    UNION ALL SELECT 2, 10, '第2天'
    UNION ALL SELECT 3, 20, '第3天'
    UNION ALL SELECT 4, 10, '第4天'
    UNION ALL SELECT 5, 20, '第5天'
    UNION ALL SELECT 6, 10, '第6天'
    UNION ALL SELECT 7, 50, '连续7天'
) AS tier
WHERE activity.code = 'DAILY_CHECK_IN'
  AND rule_config.rule_version = 1;

INSERT IGNORE INTO activity_participation_rules
    (activity_id, rule_version, points_cost, daily_limit, status, effective_from)
SELECT id, 1, 10, 3, 'ACTIVE', '2025-01-01 00:00:00.000'
FROM incentive_activities
WHERE code = 'SUMMER_LOTTERY';

INSERT IGNORE INTO activity_participation_rules
    (activity_id, rule_version, points_cost, daily_limit, status, effective_from)
SELECT id, 1, 0, NULL, 'ACTIVE', '2025-01-01 00:00:00.000'
FROM incentive_activities
WHERE code = 'POINTS_MALL';

INSERT IGNORE INTO lottery_prizes
    (activity_id, rule_id, prize_id, prize_name_snapshot, prize_type_snapshot,
     cover_url_snapshot, award_payload_snapshot, weight, campaign_quota,
     display_order)
SELECT activity.id, rule_config.id, award.id, award.name, award.award_type,
       award.cover_url, award.award_payload,
       CASE award.code
           WHEN 'WELCOME_COUPON' THEN 1
           WHEN 'BONUS_POINTS_100' THEN 3
           ELSE 6
       END,
       CASE WHEN award.award_type = 'NONE' THEN NULL ELSE award.available_stock END,
       CASE award.code
           WHEN 'WELCOME_COUPON' THEN 1
           WHEN 'BONUS_POINTS_100' THEN 2
           ELSE 3
       END
FROM incentive_activities AS activity
JOIN activity_participation_rules AS rule_config
  ON rule_config.activity_id = activity.id
JOIN award_db.awards AS award
  ON award.code IN ('WELCOME_COUPON', 'BONUS_POINTS_100', 'THANKS')
WHERE activity.code = 'SUMMER_LOTTERY'
  AND rule_config.rule_version = 1;

SET @summer_activity_id = (
    SELECT id FROM incentive_activities WHERE code = 'SUMMER_LOTTERY' LIMIT 1
);
SET @summer_rule_id = (
    SELECT id FROM activity_participation_rules
    WHERE activity_id = @summer_activity_id AND rule_version = 1 LIMIT 1
);
SET @welcome_award_id = (
    SELECT id FROM award_db.awards WHERE code = 'WELCOME_COUPON' LIMIT 1
);
SET @bonus_award_id = (
    SELECT id FROM award_db.awards WHERE code = 'BONUS_POINTS_100' LIMIT 1
);
SET @thanks_award_id = (
    SELECT id FROM award_db.awards WHERE code = 'THANKS' LIMIT 1
);

-- 前置责任链：名单短路、抽奖次数解锁、积分成本调权，最后配置幸运奖兜底。
INSERT INTO lottery_pre_draw_rules
    (activity_id, participation_rule_id, rule_type, execution_order, enabled, rule_config)
VALUES
    (@summer_activity_id, @summer_rule_id, 'USER_LIST', 10, 1,
     JSON_ARRAY(@black_user_id)),
    (@summer_activity_id, @summer_rule_id, 'PRIZE_UNLOCK', 20, 1,
     JSON_OBJECT(CAST(@welcome_award_id AS CHAR), 3)),
    (@summer_activity_id, @summer_rule_id, 'POINTS_WEIGHT', 30, 1,
     JSON_OBJECT('10', JSON_OBJECT(CAST(@bonus_award_id AS CHAR), 2))),
    (@summer_activity_id, @summer_rule_id, 'LUCKY_FALLBACK', 2147483647, 1,
     JSON_EXTRACT(JSON_ARRAY(@thanks_award_id), '$[0]'))
ON DUPLICATE KEY UPDATE
    execution_order = VALUES(execution_order),
    enabled = VALUES(enabled),
    rule_config = VALUES(rule_config);

INSERT IGNORE INTO redemption_items
    (activity_id, rule_id, item_code, prize_id, prize_name_snapshot,
     prize_type_snapshot, cover_url_snapshot, award_payload_snapshot,
     points_price, campaign_quota, display_order, eligibility_rule, status)
SELECT activity.id, rule_config.id, award.code, award.id, award.name,
       award.award_type, award.cover_url, award.award_payload,
       CASE award.code
           WHEN 'COFFEE_COUPON' THEN 120
           WHEN 'VIDEO_VIP_30D' THEN 300
           ELSE 800
       END,
       CASE award.code
           WHEN 'COFFEE_COUPON' THEN 200
           WHEN 'VIDEO_VIP_30D' THEN 100
           ELSE 50
       END,
       CASE award.code
           WHEN 'COFFEE_COUPON' THEN 1
           WHEN 'VIDEO_VIP_30D' THEN 2
           ELSE 3
       END,
       JSON_OBJECT(), 'ACTIVE'
FROM incentive_activities AS activity
JOIN activity_participation_rules AS rule_config
  ON rule_config.activity_id = activity.id
JOIN award_db.awards AS award
  ON award.code IN ('COFFEE_COUPON', 'VIDEO_VIP_30D', 'SHOPPING_CARD_50')
WHERE activity.code = 'POINTS_MALL'
  AND rule_config.rule_version = 1;

-- ---------------------------------------------------------------------------
-- 初始化结果核对
-- ---------------------------------------------------------------------------
SELECT username, nickname, status
FROM user_db.users
ORDER BY id;

SELECT activity.code, activity.activity_type, activity.status,
       COUNT(DISTINCT prize.id) AS lottery_prize_count,
       COUNT(DISTINCT item.id) AS redemption_item_count,
       COUNT(DISTINCT pre_rule.id) AS pre_draw_rule_count
FROM incentive_db.incentive_activities AS activity
LEFT JOIN incentive_db.lottery_prizes AS prize ON prize.activity_id = activity.id
LEFT JOIN incentive_db.redemption_items AS item ON item.activity_id = activity.id
LEFT JOIN incentive_db.lottery_pre_draw_rules AS pre_rule
  ON pre_rule.activity_id = activity.id
GROUP BY activity.id, activity.code, activity.activity_type, activity.status
ORDER BY activity.id;
