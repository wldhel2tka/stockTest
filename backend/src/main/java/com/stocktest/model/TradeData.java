package com.stocktest.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TradeData {
    private String code;
    private String time;
    private long price;
    private long change;
    private double changeRate;
    private long volume;
    private String side; // 매수 / 매도 / 장전
}
