CREATE TABLE IF NOT EXISTS users (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    username    VARCHAR(50)  NOT NULL UNIQUE COMMENT '아이디',
    password    VARCHAR(255) NOT NULL COMMENT '비밀번호',
    name        VARCHAR(50)  NOT NULL COMMENT '이름',
    company     VARCHAR(100) COMMENT '회사',
    phone       VARCHAR(20)  COMMENT '연락처',
    email       VARCHAR(100) NOT NULL UNIQUE COMMENT '이메일',
    user_type   ENUM('CUSTOMER', 'DEVELOPER') NOT NULL DEFAULT 'CUSTOMER' COMMENT '고객사/개발사',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='회원';

CREATE TABLE IF NOT EXISTS virtual_account (
    id         BIGINT NOT NULL AUTO_INCREMENT,
    user_id    BIGINT NOT NULL UNIQUE,
    balance    BIGINT NOT NULL DEFAULT 10000000,
    PRIMARY KEY (id),
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='가상 계좌';

CREATE TABLE IF NOT EXISTS portfolio (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    user_id     BIGINT      NOT NULL,
    stock_code  VARCHAR(10) NOT NULL,
    stock_name  VARCHAR(50) NOT NULL,
    quantity    INT         NOT NULL DEFAULT 0,
    avg_price   BIGINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_stock (user_id, stock_code),
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='보유 종목';

CREATE TABLE IF NOT EXISTS transactions (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    user_id      BIGINT      NOT NULL,
    type         ENUM('BUY','SELL') NOT NULL,
    stock_code   VARCHAR(10) NOT NULL,
    stock_name   VARCHAR(50) NOT NULL,
    quantity     INT         NOT NULL,
    price        BIGINT      NOT NULL,
    total_amount BIGINT      NOT NULL,
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='거래 내역';
