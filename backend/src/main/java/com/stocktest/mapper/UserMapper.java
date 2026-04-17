package com.stocktest.mapper;

import com.stocktest.model.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {
    User findByUsername(String username);
    User findByEmail(String email);
    void insert(User user);
}
