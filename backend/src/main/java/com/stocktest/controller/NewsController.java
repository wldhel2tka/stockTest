package com.stocktest.controller;

import com.stocktest.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;

    @GetMapping("/analysis")
    public ResponseEntity<?> analysis(@RequestParam(defaultValue = "false") boolean refresh) {
        return ResponseEntity.ok(newsService.getNewsAnalysis(refresh));
    }
}
