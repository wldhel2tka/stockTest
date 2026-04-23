package com.stocktest.controller;

import com.stocktest.service.TechnicalAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class TechnicalAnalysisController {

    private final TechnicalAnalysisService analysisService;

    @GetMapping("/signal")
    public ResponseEntity<?> signal(@RequestParam String code) {
        try {
            return ResponseEntity.ok(analysisService.analyze(code));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
