package com.stocktest.controller;

import com.stocktest.service.AutoTradingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auto-trading")
@RequiredArgsConstructor
public class AutoTradingController {

    private final AutoTradingService service;

    @GetMapping("/status")
    public ResponseEntity<?> status() {
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("enabled",       service.isEnabled());
        result.put("takeProfitPct", service.getTakeProfitPct());
        result.put("stopLossPct",   service.getStopLossPct());
        result.put("buyAmount",     service.getBuyAmount());
        result.put("lastRunTime",   service.getLastRunTime() != null ? service.getLastRunTime().toString() : null);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/toggle")
    public ResponseEntity<?> toggle(@RequestBody Map<String, Object> body) {
        boolean enable = Boolean.parseBoolean(body.get("enabled").toString());
        service.setEnabled(enable);
        return ResponseEntity.ok(Map.of("enabled", service.isEnabled()));
    }

    @PostMapping("/config")
    public ResponseEntity<?> config(@RequestBody Map<String, Object> body) {
        if (body.containsKey("takeProfitPct"))
            service.setTakeProfitPct(Double.parseDouble(body.get("takeProfitPct").toString()));
        if (body.containsKey("stopLossPct"))
            service.setStopLossPct(Double.parseDouble(body.get("stopLossPct").toString()));
        if (body.containsKey("buyAmount"))
            service.setBuyAmount(Long.parseLong(body.get("buyAmount").toString()));
        return ResponseEntity.ok(Map.of(
            "takeProfitPct", service.getTakeProfitPct(),
            "stopLossPct",   service.getStopLossPct(),
            "buyAmount",     service.getBuyAmount()
        ));
    }

    @GetMapping("/logs")
    public ResponseEntity<?> logs() {
        return ResponseEntity.ok(service.getLogs());
    }

    @PostMapping("/run-now")
    public ResponseEntity<?> runNow() {
        service.run();
        return ResponseEntity.ok(Map.of("message", "수동 실행 완료"));
    }
}
