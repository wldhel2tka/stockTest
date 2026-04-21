package com.stocktest.service;

import com.stocktest.model.TradeData;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class KisWebSocketService {

    @Value("${kis.ws-url:ws://ops.koreainvestment.com:21000}")
    private String wsUrl;

    private final KisApprovalKeyService approvalKeyService;
    private final SseService sseService;

    private volatile WebSocket webSocket;
    private final Set<String> subscribedCodes = ConcurrentHashMap.newKeySet();
    private volatile boolean connecting = false;

    private synchronized void ensureConnected() {
        if (webSocket != null && !webSocket.isOutputClosed()) return;
        if (connecting) return;
        connecting = true;
        try {
            String approvalKey = approvalKeyService.getApprovalKey();
            HttpClient client = HttpClient.newHttpClient();
            webSocket = client.newWebSocketBuilder()
                    .buildAsync(URI.create(wsUrl), new KisListener())
                    .join();
            log.info("KIS WebSocket 연결 완료: {}", wsUrl);

            for (String code : subscribedCodes) {
                sendSubscribeMessage(code, approvalKey);
            }
        } catch (Exception e) {
            log.error("KIS WebSocket 연결 실패: {}", e.getMessage());
            webSocket = null;
        } finally {
            connecting = false;
        }
    }

    public void subscribe(String stockCode) {
        ensureConnected();
        if (webSocket == null) return;
        if (subscribedCodes.add(stockCode)) {
            sendSubscribeMessage(stockCode, approvalKeyService.getApprovalKey());
        }
    }

    private void sendSubscribeMessage(String code, String approvalKey) {
        if (webSocket == null || webSocket.isOutputClosed()) return;
        String msg = """
                {"header":{"approval_key":"%s","custtype":"P","tr_type":"1","content-type":"utf-8"},"body":{"input":{"tr_id":"H0STCNT0","tr_key":"%s"}}}"""
                .formatted(approvalKey, code);
        webSocket.sendText(msg, true);
        log.info("실시간 체결 구독 요청: {}", code);
    }

    private void handleMessage(String raw) {
        if (raw.startsWith("{")) {
            log.debug("KIS 제어 메시지: {}", raw.length() > 120 ? raw.substring(0, 120) : raw);
            return;
        }

        // "암호화여부|TR_ID|건수|데이터"
        String[] parts = raw.split("\\|", 4);
        if (parts.length < 4) return;

        String trId = parts[1];

        if ("PINGPONG".equals(trId)) {
            if (webSocket != null) webSocket.sendText(raw, true);
            return;
        }

        if (!"H0STCNT0".equals(trId)) return;
        if ("1".equals(parts[0])) {
            log.warn("암호화된 메시지 수신 - 처리 불가");
            return;
        }

        String[] fields = parts[3].split("\\^");
        if (fields.length < 22) return;

        try {
            sseService.broadcast(fields[0], parseTradeData(fields));
        } catch (Exception e) {
            log.warn("체결 데이터 파싱 오류: {}", e.getMessage());
        }
    }

    private TradeData parseTradeData(String[] f) {
        String rawTime = f[1];
        String time = rawTime.length() >= 6
                ? rawTime.substring(0, 2) + ":" + rawTime.substring(2, 4) + ":" + rawTime.substring(4, 6)
                : rawTime;

        int signCode = Integer.parseInt(f[3]); // 2=상승, 3=보합, 5=하락
        boolean falling = signCode == 4 || signCode == 5;
        long changeAbs = Long.parseLong(f[4]);
        double changeRateAbs = Double.parseDouble(f[5]);

        String side = switch (f[21]) {
            case "1" -> "매도";
            case "5" -> "매수";
            default -> "장전";
        };

        return TradeData.builder()
                .code(f[0])
                .time(time)
                .price(Long.parseLong(f[2]))
                .change(falling ? -changeAbs : changeAbs)
                .changeRate(falling ? -changeRateAbs : changeRateAbs)
                .volume(Long.parseLong(f[12]))
                .side(side)
                .build();
    }

    @PreDestroy
    public void disconnect() {
        if (webSocket != null && !webSocket.isOutputClosed()) {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "shutdown");
        }
    }

    private class KisListener implements WebSocket.Listener {
        private final StringBuilder buffer = new StringBuilder();

        @Override
        public void onOpen(WebSocket ws) {
            ws.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
            buffer.append(data);
            if (last) {
                handleMessage(buffer.toString());
                buffer.setLength(0);
            }
            ws.request(1);
            return null;
        }

        @Override
        public void onError(WebSocket ws, Throwable error) {
            log.error("KIS WebSocket 에러: {}", error.getMessage());
            webSocket = null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
            log.warn("KIS WebSocket 종료: {} {}", statusCode, reason);
            webSocket = null;
            return null;
        }
    }
}
