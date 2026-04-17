package com.stocktest.model;

import lombok.Data;

@Data
public class Portfolio {
    private Long id;
    private Long userId;
    private String stockCode;
    private String stockName;
    private Integer quantity;
    private Long avgPrice;
}
