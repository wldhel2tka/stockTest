package com.stocktest.controller;

import com.stocktest.service.MockInvestmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/mock")
@RequiredArgsConstructor
public class MockInvestmentController {

    private final MockInvestmentService service;

    @GetMapping("/account")
    public ResponseEntity<?> getAccount(@RequestParam Long userId) {
        try {
            return ResponseEntity.ok(service.getOrCreateAccount(userId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/portfolio")
    public ResponseEntity<?> getPortfolio(@RequestParam Long userId) {
        try {
            return ResponseEntity.ok(service.getPortfolio(userId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/buy")
    public ResponseEntity<?> buy(@RequestBody Map<String, Object> body) {
        try {
            Long userId = Long.valueOf(body.get("userId").toString());
            String code = (String) body.get("code");
            int quantity = Integer.parseInt(body.get("quantity").toString());
            service.buy(userId, code, quantity);
            return ResponseEntity.ok(Map.of("message", "매수 완료"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/sell")
    public ResponseEntity<?> sell(@RequestBody Map<String, Object> body) {
        try {
            Long userId = Long.valueOf(body.get("userId").toString());
            String code = (String) body.get("code");
            int quantity = Integer.parseInt(body.get("quantity").toString());
            service.sell(userId, code, quantity);
            return ResponseEntity.ok(Map.of("message", "매도 완료"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/transactions")
    public ResponseEntity<?> getTransactions(@RequestParam Long userId) {
        try {
            return ResponseEntity.ok(service.getTransactions(userId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/reset")
    public ResponseEntity<?> reset(@RequestBody Map<String, Object> body) {
        try {
            Long userId = Long.valueOf(body.get("userId").toString());
            service.reset(userId);
            return ResponseEntity.ok(Map.of("message", "계좌가 초기화되었습니다. (잔고 10,000,000원)"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
