package com.stocktest.model;

import lombok.Data;

@Data
public class StockSignal {
    private String code;
    private String name;

    // RSI
    private double rsi;
    private String rsiSignal;   // BUY / SELL / NEUTRAL

    // 이동평균
    private long ma5;
    private long ma20;
    private String maSignal;    // BUY / SELL / NEUTRAL

    // 거래량
    private double volumeRatio; // 오늘 거래량 / 20일 평균
    private String volumeSignal; // HIGH / NORMAL

    // 종합
    private String signal;      // BUY / SELL / NEUTRAL
    private int score;          // 양수=매수우세, 음수=매도우세
    private String description;
}
