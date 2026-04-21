package com.stocktest.controller;

import com.stocktest.service.KisWebSocketService;
import com.stocktest.service.SseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/realtime")
@RequiredArgsConstructor
public class RealtimeController {

    private final KisWebSocketService kisWebSocketService;
    private final SseService sseService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam String code) {
        kisWebSocketService.subscribe(code);
        return sseService.createEmitter(code);
    }
}
