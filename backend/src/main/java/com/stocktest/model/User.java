package com.stocktest.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {
    private Long id;
    private String username;   // 아이디
    private String password;   // 비밀번호
    private String name;       // 이름
    private String company;    // 회사
    private String phone;      // 연락처
    private String email;      // 이메일
    private String userType;   // 고객사(CUSTOMER) / 개발사(DEVELOPER)
    private LocalDateTime createdAt;
}
