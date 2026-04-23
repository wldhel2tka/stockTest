package com.stocktest.controller;

import com.stocktest.service.RealOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/real")
@RequiredArgsConstructor
public class RealOrderController {

    private final RealOrderService realOrderService;

    /**
     * 매수/매도 주문
     * body: { code, quantity, side("BUY"|"SELL"), orderType("MARKET"|"LIMIT"), price(지정가일 때) }
     */
    @PostMapping("/order")
    public ResponseEntity<?> order(@RequestBody Map<String, Object> body) {
        try {
            String code = (String) body.get("code");
            int quantity = Integer.parseInt(body.get("quantity").toString());
            String side = ((String) body.get("side")).toUpperCase();
            String orderType = body.containsKey("orderType")
                    ? ((String) body.get("orderType")).toUpperCase() : "MARKET";
            long price = body.containsKey("price")
                    ? Long.parseLong(body.get("price").toString()) : 0L;

            if (code == null || code.isBlank()) return ResponseEntity.badRequest().body(Map.of("message", "종목코드 필요"));
            if (quantity <= 0) return ResponseEntity.badRequest().body(Map.of("message", "수량은 1주 이상"));
            if (!side.equals("BUY") && !side.equals("SELL")) return ResponseEntity.badRequest().body(Map.of("message", "side는 BUY 또는 SELL"));

            Map<String, Object> result = realOrderService.order(code, quantity, side, orderType, price);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** 실계좌 잔고 조회 */
    @GetMapping("/balance")
    public ResponseEntity<?> balance() {
        try {
            return ResponseEntity.ok(realOrderService.getBalance());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** 일별 주문체결 내역 */
    @GetMapping("/orders")
    public ResponseEntity<?> orders() {
        try {
            return ResponseEntity.ok(realOrderService.getOrderHistory());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
