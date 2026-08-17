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

CREATE TABLE IF NOT EXISTS roles (
    code VARCHAR(32) NOT NULL,
    name VARCHAR(64) NOT NULL,
    PRIMARY KEY (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='RBAC roles';

CREATE TABLE IF NOT EXISTS permissions (
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    PRIMARY KEY (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='RBAC permissions';

-- Current policy assigns one role per user; the join table keeps the RBAC model extensible.
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT UNSIGNED NOT NULL,
    role    VARCHAR(32) NOT NULL,
    PRIMARY KEY (user_id),
    KEY idx_user_roles_role (role),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role) REFERENCES roles (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='user roles';

CREATE TABLE IF NOT EXISTS role_permissions (
    role_code      VARCHAR(32) NOT NULL,
    permission_code VARCHAR(64) NOT NULL,
    PRIMARY KEY (role_code, permission_code),
    KEY idx_role_permissions_permission (permission_code),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_code) REFERENCES roles (code),
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_code) REFERENCES permissions (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='role permission grants';

INSERT IGNORE INTO roles (code, name) VALUES
    ('USER', '用户'),
    ('ADMIN', '管理员'),
    ('SUPER_ADMIN', '超级管理员');

INSERT IGNORE INTO permissions (code, name) VALUES
    ('ACCOUNT_SELF', '查看和修改本人资料'),
    ('POINTS_SELF', '查看本人积分'),
    ('CHECK_IN', '参与签到'),
    ('LOTTERY_PARTICIPATE', '参与抽奖'),
    ('REDEMPTION_PARTICIPATE', '参与兑换'),
    ('ACTIVITY_MANAGE', '管理活动'),
    ('PRIZE_MANAGE', '管理奖品'),
    ('INVENTORY_MANAGE', '管理库存'),
    ('ROLE_MANAGE', '管理用户角色');

INSERT IGNORE INTO role_permissions (role_code, permission_code) VALUES
    ('USER', 'ACCOUNT_SELF'),
    ('USER', 'POINTS_SELF'),
    ('USER', 'CHECK_IN'),
    ('USER', 'LOTTERY_PARTICIPATE'),
    ('USER', 'REDEMPTION_PARTICIPATE'),
    ('ADMIN', 'ACCOUNT_SELF'),
    ('ADMIN', 'ACTIVITY_MANAGE'),
    ('ADMIN', 'PRIZE_MANAGE'),
    ('ADMIN', 'INVENTORY_MANAGE'),
    ('SUPER_ADMIN', 'ACCOUNT_SELF'),
    ('SUPER_ADMIN', 'POINTS_SELF'),
    ('SUPER_ADMIN', 'CHECK_IN'),
    ('SUPER_ADMIN', 'LOTTERY_PARTICIPATE'),
    ('SUPER_ADMIN', 'REDEMPTION_PARTICIPATE'),
    ('SUPER_ADMIN', 'ACTIVITY_MANAGE'),
    ('SUPER_ADMIN', 'PRIZE_MANAGE'),
    ('SUPER_ADMIN', 'INVENTORY_MANAGE'),
    ('SUPER_ADMIN', 'ROLE_MANAGE');

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
