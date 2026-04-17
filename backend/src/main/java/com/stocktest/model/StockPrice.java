package com.stocktest.model;

import lombok.Data;

@Data
public class StockPrice {
    private String code;
    private String name;
    private long currentPrice;
    private long change;
    private double changeRate;
    private long openPrice;
    private long highPrice;
    private long lowPrice;
    private long volume;
    private boolean up;
}
