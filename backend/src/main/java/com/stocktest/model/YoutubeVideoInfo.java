package com.stocktest.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YoutubeVideoInfo {
    private String videoId;
    private String title;
    private String publishedAt;
    private String thumbnailUrl;
    private String description;
}
