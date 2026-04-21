package com.stocktest.model;

import lombok.Data;

@Data
public class ChartCandle {
    private String date;      // 일봉용 (YYYY-MM-DD)
    private Long timestamp;   // 분봉용 (Unix seconds, KST)
    private long open;
    private long high;
    private long low;
    private long close;
    private long volume;
}
