package com.stocktest.service;

import com.stocktest.mapper.MockInvestmentMapper;
import com.stocktest.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutoTradingService {

    private final MockInvestmentService mockInvestmentService;
    private final TechnicalAnalysisService analysisService;
    private final StockService stockService;
    private final MockInvestmentMapper mapper;

    // 설정
    private volatile boolean enabled = false;
    private volatile double takeProfitPct = 10.0;   // 익절 기준 (%)
    private volatile double stopLossPct   = 5.0;    // 손절 기준 (%)
    private volatile long   buyAmount     = 1_000_000L;

    private final List<AutoTradeLog> logs = new CopyOnWriteArrayList<>();
    private volatile LocalDateTime lastRunTime = null;
    private static final int MAX_LOGS = 100;

    private static final List<String> WATCH_LIST = List.of(
        "005930","000660","035420","005380","000270",
        "068270","035720","105560","055550","086790",
        "323410","259960","247540","086520","196170",
        "214150","145020","329180","003670","003550"
    );

    @Scheduled(fixedDelay = 60_000)
    public void run() {
        if (!enabled) return;
        if (!isMarketHours()) return;

        lastRunTime = LocalDateTime.now();
        log.info("[자동매매] 실행 - 익절{}% 손절{}% 매수금액{}원", takeProfitPct, stopLossPct, buyAmount);

        List<VirtualAccount> accounts = getAllAccounts();
        for (VirtualAccount account : accounts) {
            try { autoSell(account.getUserId()); } catch (Exception e) {
                log.error("[자동매매] 매도 오류 userId={}: {}", account.getUserId(), e.getMessage());
            }
            try { autoBuy(account.getUserId()); } catch (Exception e) {
                log.error("[자동매매] 매수 오류 userId={}: {}", account.getUserId(), e.getMessage());
            }
        }
    }

    private void autoSell(Long userId) {
        List<Portfolio> holdings = mapper.findPortfolioByUserId(userId);
        for (Portfolio p : holdings) {
            try {
                StockPrice price = stockService.getPrice(p.getStockCode());
                long current = price.getCurrentPrice();
                long avg = p.getAvgPrice();

                double pnlPct = (double)(current - avg) / avg * 100;

                if (pnlPct >= takeProfitPct) {
                    mockInvestmentService.sell(userId, p.getStockCode(), p.getQuantity());
                    addLog("SELL_PROFIT", p.getStockCode(), p.getStockName(), current, p.getQuantity(),
                        String.format("익절: +%.1f%% (목표 +%.1f%%)", pnlPct, takeProfitPct));
                    log.info("[자동매매] 익절 {} {} +{:.1f}%", p.getStockName(), p.getQuantity(), pnlPct);
                } else if (pnlPct <= -stopLossPct) {
                    mockInvestmentService.sell(userId, p.getStockCode(), p.getQuantity());
                    addLog("SELL_LOSS", p.getStockCode(), p.getStockName(), current, p.getQuantity(),
                        String.format("손절: %.1f%% (기준 -%.1f%%)", pnlPct, stopLossPct));
                    log.info("[자동매매] 손절 {} {} {:.1f}%", p.getStockName(), p.getQuantity(), pnlPct);
                }
            } catch (Exception e) {
                log.warn("[자동매매] 가격조회 실패 {}: {}", p.getStockCode(), e.getMessage());
            }
        }
    }

    private void autoBuy(Long userId) {
        VirtualAccount account = mockInvestmentService.getOrCreateAccount(userId);
        if (account.getBalance() < buyAmount) return;

        Set<String> holding = new HashSet<>();
        mapper.findPortfolioByUserId(userId).forEach(p -> holding.add(p.getStockCode()));

        // 매수 횟수 제한: 1회 실행당 최대 2종목
        int bought = 0;
        for (String code : WATCH_LIST) {
            if (bought >= 2) break;
            if (holding.contains(code)) continue;

            try {
                StockSignal signal = analysisService.analyze(code);
                if (!"BUY".equals(signal.getSignal())) continue;

                StockPrice price = stockService.getPrice(code);
                if (price.getCurrentPrice() <= 0) continue;

                int qty = (int)(buyAmount / price.getCurrentPrice());
                if (qty < 1) continue;

                // 잔고 재확인
                account = mockInvestmentService.getOrCreateAccount(userId);
                if (account.getBalance() < price.getCurrentPrice() * qty) continue;

                mockInvestmentService.buy(userId, code, qty);
                String name = stockService.getNameByCode(code);
                addLog("BUY", code, name, price.getCurrentPrice(), qty,
                    String.format("자동매수: %s (점수%+d, RSI%.1f)", signal.getDescription(), signal.getScore(), signal.getRsi()));
                log.info("[자동매매] 매수 {} {}주 @{}원", name, qty, price.getCurrentPrice());
                bought++;
            } catch (Exception e) {
                log.warn("[자동매매] 매수 실패 {}: {}", code, e.getMessage());
            }
        }
    }

    private boolean isMarketHours() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));
        DayOfWeek day = now.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) return false;
        LocalTime t = now.toLocalTime();
        return !t.isBefore(LocalTime.of(9, 0)) && t.isBefore(LocalTime.of(15, 30));
    }

    private List<VirtualAccount> getAllAccounts() {
        // 현재는 userId=1 고정 (추후 확장 가능)
        VirtualAccount account = mapper.findAccountByUserId(1L);
        if (account == null) return List.of();
        return List.of(account);
    }

    private void addLog(String type, String code, String name, long price, int qty, String reason) {
        logs.add(0, new AutoTradeLog(
            LocalDateTime.now(), type, code, name, price, qty, price * qty, reason
        ));
        if (logs.size() > MAX_LOGS) logs.subList(MAX_LOGS, logs.size()).clear();
    }

    // Getters / Setters for controller
    public boolean isEnabled()           { return enabled; }
    public void setEnabled(boolean v)    { enabled = v; }
    public double getTakeProfitPct()     { return takeProfitPct; }
    public void setTakeProfitPct(double v) { takeProfitPct = v; }
    public double getStopLossPct()       { return stopLossPct; }
    public void setStopLossPct(double v) { stopLossPct = v; }
    public long getBuyAmount()           { return buyAmount; }
    public void setBuyAmount(long v)     { buyAmount = v; }
    public List<AutoTradeLog> getLogs()  { return logs; }
    public LocalDateTime getLastRunTime(){ return lastRunTime; }
}
