CREATE TABLE teams_conversation (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL UNIQUE,
    conversation_id VARCHAR(255) NOT NULL,
    service_url     VARCHAR(512) NOT NULL,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255)
);
