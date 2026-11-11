USE award_db;

ALTER TABLE award_issuances MODIFY source_type
    ENUM('LOTTERY', 'REDEMPTION', 'SHOP_REDEMPTION') NOT NULL COMMENT '发奖来源';

ALTER TABLE user_awards MODIFY source_type
    ENUM('LOTTERY', 'REDEMPTION', 'SHOP_REDEMPTION') NOT NULL COMMENT '获得来源';

CREATE TABLE IF NOT EXISTS shop_redemptions (
    id                   BIGINT UNSIGNED NOT NULL COMMENT '兑换单ID及扣积分业务号',
    user_id              BIGINT UNSIGNED NOT NULL,
    request_id           VARCHAR(64) COLLATE utf8mb4_0900_bin NOT NULL,
    award_id             BIGINT UNSIGNED NOT NULL,
    award_name           VARCHAR(100) NOT NULL,
    award_type           ENUM('VIRTUAL', 'POINTS') NOT NULL,
    award_payload        JSON NULL,
    points_cost          BIGINT UNSIGNED NOT NULL,
    status               ENUM('RESERVED', 'PAID', 'COMPLETED', 'REJECTED') NOT NULL,
    point_transaction_id BIGINT UNSIGNED NULL,
    balance_after        BIGINT UNSIGNED NULL,
    created_at           DATETIME(3) NOT NULL,
    next_attempt_at      DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_shop_redemptions_request (user_id, request_id),
    KEY idx_shop_redemptions_due (status, next_attempt_at, id),
    CONSTRAINT fk_shop_redemptions_award FOREIGN KEY (award_id) REFERENCES awards (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
