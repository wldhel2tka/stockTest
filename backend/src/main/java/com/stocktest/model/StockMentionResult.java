package com.stocktest.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockMentionResult {
    private String code;
    private String name;
    private long currentPrice;
    private long change;
    private double changeRate;
    private boolean up;
}
