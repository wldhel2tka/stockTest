package com.stocktest.model;

import lombok.Data;
import java.util.List;

@Data
public class PortfolioResponse {
    private long balance;
    private long totalInvested;
    private long totalCurrentValue;
    private long totalAssets;
    private long totalPnl;
    private double totalPnlRate;
    private List<PortfolioItem> holdings;
}
