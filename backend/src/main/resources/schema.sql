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
