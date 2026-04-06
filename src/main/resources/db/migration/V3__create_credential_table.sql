CREATE TABLE auth_schema.credentials
(
    id              BIGSERIAL PRIMARY KEY,
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
