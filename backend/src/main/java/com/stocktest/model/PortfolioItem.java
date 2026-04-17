package com.stocktest.model;

import lombok.Data;

@Data
public class PortfolioItem {
    private String stockCode;
    private String stockName;
    private int quantity;
    private long avgPrice;
    private long totalInvested;
    private long currentPrice;
    private long currentValue;
    private long pnl;
    private double pnlRate;
}
