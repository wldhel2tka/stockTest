package com.stocktest.model;

import lombok.Data;

@Data
public class ChartCandle {
    private String date;
    private long open;
    private long high;
    private long low;
    private long close;
    private long volume;
}
