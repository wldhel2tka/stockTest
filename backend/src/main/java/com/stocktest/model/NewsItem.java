package com.stocktest.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsItem {
    private String title;
    private String link;
    private String source;
    private String publishedAt;
    private List<StockMentionResult> relatedStocks;
}
