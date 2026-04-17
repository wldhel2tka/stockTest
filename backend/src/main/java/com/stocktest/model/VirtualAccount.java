package com.stocktest.model;

import lombok.Data;

@Data
public class VirtualAccount {
    private Long id;
    private Long userId;
    private Long balance;
}
