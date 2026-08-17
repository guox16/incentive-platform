USE user_db;

-- Identity, roles, and membership are owned by user-service.
CREATE TABLE IF NOT EXISTS users (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'user id',
    username      VARCHAR(64) NOT NULL COMMENT 'login name',
    password_hash VARCHAR(255) NOT NULL COMMENT 'password hash',
    nickname      VARCHAR(64) NOT NULL COMMENT 'display name',
    mobile        VARCHAR(20) NULL,
    status        TINYINT NOT NULL DEFAULT 1 COMMENT '1 active, 0 disabled',
    created_at    DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at    DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username),
    UNIQUE KEY uk_users_mobile (mobile)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='users';

-- Only a SHA-256 digest is persisted. Raw refresh tokens exist only in HttpOnly cookies.
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id      BIGINT UNSIGNED NOT NULL,
    token_hash   CHAR(64) NOT NULL,
    token_family VARCHAR(36) NOT NULL COMMENT 'rotation family used for replay revocation',
    expires_at   DATETIME(3) NOT NULL,
    revoked_at   DATETIME(3) NULL,
    created_at   DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_refresh_tokens_hash (token_hash),
    KEY idx_refresh_tokens_user (user_id),
    KEY idx_refresh_tokens_family (token_family),
    KEY idx_refresh_tokens_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='rotating refresh tokens';

-- V0.1 contains USER and ADMIN; the relation keeps role assignment extensible.
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT UNSIGNED NOT NULL,
    role    VARCHAR(32) NOT NULL,
    PRIMARY KEY (user_id, role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='user roles';

-- The current membership state is updated atomically by user_id.
CREATE TABLE IF NOT EXISTS user_memberships (
    user_id    BIGINT UNSIGNED NOT NULL,
    expires_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_id),
    KEY idx_user_memberships_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='current memberships';

-- An immutable membership history; business_no provides award-delivery idempotency.
CREATE TABLE IF NOT EXISTS membership_records (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id     BIGINT UNSIGNED NOT NULL,
    business_no VARCHAR(64) NOT NULL COMMENT 'external idempotency key',
    change_days INT NOT NULL,
    expires_at  DATETIME(3) NOT NULL COMMENT 'expiry after this change',
    created_at  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_membership_records_business_no (business_no),
    KEY idx_membership_records_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='membership history';
