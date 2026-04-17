package com.stocktest.model;

import lombok.Data;

@Data
public class RecommendedStock {
    private int rank;
    private String code;
    private String name;
    private long currentPrice;
    private long change;
    private double changeRate;
    private long volume;
    private long tradingValue;  // 거래대금
    private boolean up;
}
