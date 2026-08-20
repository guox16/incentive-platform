-- 已存在 incentive_db.redemption_records 时执行一次；全新环境由 init/05 脚本直接建成新结构。
USE incentive_db;

ALTER TABLE redemption_records
    ADD COLUMN request_id VARCHAR(64) NULL COMMENT '客户端兑换请求幂等号' AFTER id,
    ADD COLUMN activity_code_snapshot VARCHAR(64) NULL COMMENT '活动编码快照' AFTER activity_id,
    ADD COLUMN point_business_id BIGINT UNSIGNED NULL COMMENT '积分扣减稳定业务幂等号'
        AFTER eligibility_result,
    MODIFY COLUMN point_transaction_id BIGINT UNSIGNED NULL COMMENT '积分服务扣减流水ID',
    ADD COLUMN balance_after BIGINT UNSIGNED NULL COMMENT '扣减后的积分余额'
        AFTER point_transaction_id,
    ADD COLUMN status VARCHAR(16) NULL COMMENT 'PENDING待扣减、COMPLETED兑换完成'
        AFTER balance_after,
    ADD COLUMN updated_at DATETIME(3) NULL COMMENT '更新时间' AFTER created_at;

-- 历史兑换均来自旧同步流程，存在积分流水即视为已经完成。
UPDATE redemption_records record
JOIN incentive_activities activity ON activity.id = record.activity_id
SET record.request_id = CONCAT('legacy-redemption-', record.id),
    record.activity_code_snapshot = activity.code,
    record.point_business_id = record.id,
    record.status = 'COMPLETED',
    record.updated_at = record.created_at;

ALTER TABLE redemption_records
    MODIFY COLUMN request_id VARCHAR(64) NOT NULL COMMENT '客户端兑换请求幂等号',
    MODIFY COLUMN activity_code_snapshot VARCHAR(64) NOT NULL COMMENT '活动编码快照',
    MODIFY COLUMN point_business_id BIGINT UNSIGNED NOT NULL COMMENT '积分扣减稳定业务幂等号',
    MODIFY COLUMN status VARCHAR(16) NOT NULL DEFAULT 'PENDING'
        COMMENT 'PENDING待扣减、COMPLETED兑换完成',
    MODIFY COLUMN updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    ADD UNIQUE KEY uk_redemption_records_request_id (request_id),
    ADD UNIQUE KEY uk_redemption_records_point_business_id (point_business_id),
    ADD CONSTRAINT chk_redemption_records_status CHECK (status IN ('PENDING', 'COMPLETED'));
