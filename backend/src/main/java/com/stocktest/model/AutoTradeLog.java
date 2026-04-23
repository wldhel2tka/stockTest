package com.stocktest.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class AutoTradeLog {
    private LocalDateTime time;
    private String type;       // BUY / SELL_PROFIT / SELL_LOSS
    private String stockCode;
    private String stockName;
    private long price;
    private int quantity;
    private long amount;
    private String reason;
}
