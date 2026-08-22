USE incentive_db;

-- 奖品独占活动：以活动配置配额生成Redis库存编号，数据库编号负责最终防重。
UPDATE lottery_prizes AS configured
JOIN award_db.awards AS award ON award.id = configured.prize_id
SET configured.campaign_quota = award.available_stock
WHERE configured.prize_type_snapshot <> 'NONE'
  AND configured.campaign_quota IS NULL;

ALTER TABLE lottery_orders
    ADD COLUMN stock_no BIGINT UNSIGNED NULL
        COMMENT '活动奖品库存编号；NONE奖品为空' AFTER award_payload_snapshot,
    ADD UNIQUE KEY uk_lottery_orders_prize_stock (activity_id, prize_id, stock_no);

ALTER TABLE lottery_records
    ADD COLUMN stock_no BIGINT UNSIGNED NULL
        COMMENT '活动奖品库存编号；NONE奖品为空' AFTER award_payload_snapshot;

ALTER TABLE pending_awards
    ADD COLUMN stock_no BIGINT UNSIGNED NULL
        COMMENT '抽奖库存编号；兑换任务为空' AFTER award_payload_snapshot;

ALTER TABLE lottery_prizes
    ADD CONSTRAINT chk_lottery_prizes_quota CHECK (
        (prize_type_snapshot = 'NONE' AND campaign_quota IS NULL)
        OR (prize_type_snapshot <> 'NONE' AND campaign_quota IS NOT NULL)
    );
