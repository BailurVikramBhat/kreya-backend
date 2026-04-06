CREATE SCHEMA IF NOT EXISTS user_schema;
CREATE SCHEMA IF NOT EXISTS auth_schema;

CREATE TABLE IF NOT EXISTS user_schema.users
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    first_name    VARCHAR(100) NOT NULL,
    last_name     VARCHAR(100) NOT NULL,
    phone         VARCHAR(20),
    role          VARCHAR(20)  NOT NULL,
    is_vikram     BOOLEAN      NOT NULL DEFAULT FALSE,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    avatar_path   VARCHAR(500),
    last_login_at TIMESTAMP,
    created_at    TIMESTAMP    NOT NULL,
    updated_at    TIMESTAMP    NOT NULL
);

CREATE TABLE IF NOT EXISTS auth_schema.credentials
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL UNIQUE,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    email_verified  BOOLEAN      NOT NULL DEFAULT FALSE,
    mfa_enabled     BOOLEAN      NOT NULL DEFAULT FALSE,
    mfa_secret      VARCHAR(64),
    account_locked  BOOLEAN      NOT NULL DEFAULT FALSE,
    failed_attempts INT          NOT NULL DEFAULT 0,
    lock_expires_at TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL,
    updated_at      TIMESTAMP    NOT NULL,
    CONSTRAINT fk_credentials_user
        FOREIGN KEY (user_id)
            REFERENCES user_schema.users (id)
);
