package com.stocktest.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YoutubeChannel {
    private String id;
    private String name;
    private String handle;
    private String description;
    private String thumbnailUrl;
    private String channelId;
}
