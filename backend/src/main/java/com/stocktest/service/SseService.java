package com.stocktest.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocktest.model.TradeData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
public class SseService {

    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SseEmitter createEmitter(String code) {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.computeIfAbsent(code, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> remove(code, emitter));
        emitter.onTimeout(() -> remove(code, emitter));
        emitter.onError(e -> remove(code, emitter));

        log.debug("SSE 구독 추가: {} (총 {}명)", code, emitters.getOrDefault(code, List.of()).size());
        return emitter;
    }

    public void broadcast(String code, TradeData trade) {
        List<SseEmitter> list = emitters.get(code);
        if (list == null || list.isEmpty()) return;

        String json;
        try {
            json = objectMapper.writeValueAsString(trade);
        } catch (Exception e) {
            return;
        }

        List<SseEmitter> dead = new CopyOnWriteArrayList<>();
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().data(json));
            } catch (Exception e) {
                dead.add(emitter);
            }
        }
        list.removeAll(dead);
    }

    private void remove(String code, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(code);
        if (list != null) list.remove(emitter);
    }
}
