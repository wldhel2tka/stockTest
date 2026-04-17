package com.stocktest.controller;

import com.stocktest.service.RecommendedStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/recommend")
@RequiredArgsConstructor
public class RecommendedStockController {

    private final RecommendedStockService service;

    /** 거래량 상위 */
    @GetMapping("/volume")
    public ResponseEntity<?> topVolume(
            @RequestParam(defaultValue = "J") String market,
            @RequestParam(defaultValue = "0") long minPrice,
            @RequestParam(defaultValue = "0") long maxPrice,
            @RequestParam(defaultValue = "0") long minVolume) {
        try {
            return ResponseEntity.ok(service.getTopVolume(market, minPrice, maxPrice, minVolume));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** 상승률 상위 */
    @GetMapping("/gainers")
    public ResponseEntity<?> gainers(
            @RequestParam(defaultValue = "J") String market,
            @RequestParam(defaultValue = "0") long minPrice,
            @RequestParam(defaultValue = "0") long maxPrice,
            @RequestParam(defaultValue = "0") long minVolume) {
        try {
            return ResponseEntity.ok(service.getFluctuation(market, "0", minPrice, maxPrice, minVolume));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** 하락률 상위 */
    @GetMapping("/losers")
    public ResponseEntity<?> losers(
            @RequestParam(defaultValue = "J") String market,
            @RequestParam(defaultValue = "0") long minPrice,
            @RequestParam(defaultValue = "0") long maxPrice,
            @RequestParam(defaultValue = "0") long minVolume) {
        try {
            return ResponseEntity.ok(service.getFluctuation(market, "1", minPrice, maxPrice, minVolume));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
