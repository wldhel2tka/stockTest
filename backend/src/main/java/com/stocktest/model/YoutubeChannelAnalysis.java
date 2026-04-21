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
public class YoutubeChannelAnalysis {
    private YoutubeChannel channel;
    private List<YoutubeVideoInfo> recentVideos;
    private List<StockMentionResult> mentionedStocks;
    private String error;
}
