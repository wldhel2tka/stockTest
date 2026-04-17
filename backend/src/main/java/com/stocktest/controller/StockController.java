package com.stocktest.controller;

import com.stocktest.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam String query) {
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "검색어를 입력하세요."));
        }
        return ResponseEntity.ok(stockService.search(query));
    }

    @GetMapping("/price")
    public ResponseEntity<?> price(@RequestParam String code) {
        try {
            return ResponseEntity.ok(stockService.getPrice(code));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/chart")
    public ResponseEntity<?> chart(
            @RequestParam String code,
            @RequestParam(defaultValue = "D") String period,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            return ResponseEntity.ok(stockService.getChart(code, period, startDate, endDate));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
