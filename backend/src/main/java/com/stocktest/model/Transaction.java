package com.stocktest.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Transaction {
    private Long id;
    private Long userId;
    private String type;       // BUY / SELL
    private String stockCode;
    private String stockName;
    private Integer quantity;
    private Long price;
    private Long totalAmount;
    private LocalDateTime createdAt;
}
