package com.stocktest.controller;

import com.stocktest.service.YoutubeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/youtube")
@RequiredArgsConstructor
public class YoutubeController {

    private final YoutubeService youtubeService;

    @GetMapping("/channels")
    public ResponseEntity<?> channels() {
        return ResponseEntity.ok(youtubeService.getChannels());
    }

    @GetMapping("/analysis")
    public ResponseEntity<?> analysis() {
        return ResponseEntity.ok(youtubeService.getFullAnalysis());
    }

    @PostMapping("/cache/clear")
    public ResponseEntity<?> clearCache() {
        youtubeService.clearCache();
        return ResponseEntity.ok().build();
    }
}
