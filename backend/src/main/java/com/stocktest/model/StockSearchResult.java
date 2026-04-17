package com.stocktest.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockSearchResult {
    private String code;
    private String name;
    private String market;
}
