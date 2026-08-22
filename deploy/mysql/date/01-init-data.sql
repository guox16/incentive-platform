-- 本地环境全量初始化数据
-- 测试账号：modeladmin / 123456
-- 警告：执行本脚本会清空四个业务数据库中的全部现有数据，再写入初始化数据。

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------------
-- 清空全部业务数据并重置自增主键
-- TRUNCATE 会隐式提交，整个初始化过程不具备事务回滚能力。
-- ---------------------------------------------------------------------------
SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE incentive_db.pending_awards;
TRUNCATE TABLE incentive_db.redemption_records;
TRUNCATE TABLE incentive_db.lottery_records;
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
-- user_db：一个启用的普通用户
-- ---------------------------------------------------------------------------
USE user_db;

INSERT INTO users (username, password_hash, nickname, mobile, status)
VALUES (
    'modeladmin',
    '$2a$10$4MYPbxVfkO8.b/kOoWVT8.YtAGoI3s7fSCNbkQt3jjZ59vK.htAIy',
    '超级管理员',
    '13800000001',
    1
)
ON DUPLICATE KEY UPDATE
    password_hash = VALUES(password_hash),
    nickname = VALUES(nickname),
    mobile = VALUES(mobile),
    status = VALUES(status);

SET @debug_user_id = (
    SELECT id FROM users WHERE username = 'modeladmin' LIMIT 1
);

INSERT INTO user_roles (user_id, role)
VALUES (@debug_user_id, 'SUPER_ADMIN')
ON DUPLICATE KEY UPDATE role = VALUES(role);

INSERT INTO user_memberships (user_id, expires_at)
VALUES (@debug_user_id, '2035-01-01 00:00:00.000')
ON DUPLICATE KEY UPDATE expires_at = VALUES(expires_at);

-- ---------------------------------------------------------------------------
-- points_db：该用户的一个积分账户及一条初始化流水
-- ---------------------------------------------------------------------------
USE points_db;

INSERT INTO point_accounts (user_id, balance, version)
VALUES (@debug_user_id, 1000, 0)
ON DUPLICATE KEY UPDATE
    balance = VALUES(balance),
    version = VALUES(version);

INSERT INTO point_transactions
    (business_id, user_id, type, amount, balance_before, balance_after, source, remark)
VALUES
    (900000000000000001, @debug_user_id, 'CREDIT', 1000, 0, 1000,
     'DEBUG_INIT', '本地调试账户初始化积分')
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
    NULL,
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

USE incentive_db;

INSERT IGNORE INTO incentive_activities
    (code, activity_type, name, status, starts_at, ends_at)
VALUES
    ('SUMMER_LOTTERY', 'LOTTERY', '夏日幸运抽奖', 'ACTIVE',
     '2025-01-01 00:00:00.000', '2035-01-01 00:00:00.000'),
    ('POINTS_MALL', 'REDEMPTION', '积分兑换专区', 'ACTIVE',
     '2025-01-01 00:00:00.000', '2035-01-01 00:00:00.000');

INSERT IGNORE INTO activity_participation_rules
    (activity_id, rule_version, points_cost, daily_limit, status, effective_from)
SELECT id, 1, 10, 3, 'ACTIVE', '2025-01-01 00:00:00.000'
FROM incentive_activities
WHERE code = 'SUMMER_LOTTERY';

INSERT IGNORE INTO activity_participation_rules
    (activity_id, rule_version, points_cost, daily_limit, status, effective_from)
SELECT id, 1, 0, 5, 'ACTIVE', '2025-01-01 00:00:00.000'
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
       NULL,
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

INSERT IGNORE INTO redemption_items
    (activity_id, rule_id, item_code, prize_id, prize_name_snapshot,
     prize_type_snapshot, cover_url_snapshot, award_payload_snapshot,
     points_price, campaign_quota, display_order, eligibility_rule, status)
SELECT activity.id, rule_config.id, award.code, award.id, award.name,
       award.award_type, award.cover_url, award.award_payload,
       CASE award.code
           WHEN 'WELCOME_COUPON' THEN 50
           WHEN 'COFFEE_COUPON' THEN 120
           WHEN 'VIDEO_VIP_30D' THEN 300
           ELSE 800
       END,
       CASE award.code
           WHEN 'WELCOME_COUPON' THEN 500
           WHEN 'COFFEE_COUPON' THEN 200
           WHEN 'VIDEO_VIP_30D' THEN 100
           ELSE 50
       END,
       CASE award.code
           WHEN 'WELCOME_COUPON' THEN 1
           WHEN 'COFFEE_COUPON' THEN 2
           WHEN 'VIDEO_VIP_30D' THEN 3
           ELSE 4
       END,
       JSON_OBJECT(), 'ACTIVE'
FROM incentive_activities AS activity
JOIN activity_participation_rules AS rule_config
  ON rule_config.activity_id = activity.id
JOIN award_db.awards AS award
  ON award.code IN ('WELCOME_COUPON', 'COFFEE_COUPON', 'VIDEO_VIP_30D', 'SHOPPING_CARD_50')
WHERE activity.code = 'POINTS_MALL'
  AND rule_config.rule_version = 1;
