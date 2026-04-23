package com.stocktest.service;

import com.stocktest.model.ChartCandle;
import com.stocktest.model.StockSignal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TechnicalAnalysisService {

    private final StockService stockService;

    public StockSignal analyze(String code) {
        String end = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String start = LocalDate.now().minusDays(90).format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        List<ChartCandle> candles = stockService.getChart(code, "D", start, end);

        StockSignal signal = new StockSignal();
        signal.setCode(code);

        if (candles.size() < 20) {
            signal.setSignal("NEUTRAL");
            signal.setDescription("데이터 부족 (최소 20일치 필요)");
            return signal;
        }

        List<Long> closes = candles.stream().map(ChartCandle::getClose).toList();
        List<Long> volumes = candles.stream().map(ChartCandle::getVolume).toList();

        // RSI(14)
        double rsi = calcRsi(closes, 14);
        signal.setRsi(Math.round(rsi * 100.0) / 100.0);
        signal.setRsiSignal(rsi <= 30 ? "BUY" : rsi >= 70 ? "SELL" : "NEUTRAL");

        // 이동평균
        long ma5  = calcMa(closes, 5);
        long ma20 = calcMa(closes, 20);
        signal.setMa5(ma5);
        signal.setMa20(ma20);
        signal.setMaSignal(ma5 > ma20 ? "BUY" : ma5 < ma20 ? "SELL" : "NEUTRAL");

        // 거래량 (오늘 / 20일 평균)
        long todayVol = volumes.get(volumes.size() - 1);
        double avgVol = volumes.subList(volumes.size() - 21, volumes.size() - 1)
                               .stream().mapToLong(Long::longValue).average().orElse(1);
        double volRatio = avgVol > 0 ? todayVol / avgVol : 1.0;
        signal.setVolumeRatio(Math.round(volRatio * 100.0) / 100.0);
        signal.setVolumeSignal(volRatio >= 1.5 ? "HIGH" : "NORMAL");

        // 종합 점수
        int score = 0;
        if ("BUY".equals(signal.getRsiSignal()))  score++;
        if ("SELL".equals(signal.getRsiSignal())) score--;
        if ("BUY".equals(signal.getMaSignal()))   score++;
        if ("SELL".equals(signal.getMaSignal()))  score--;
        if ("HIGH".equals(signal.getVolumeSignal())) {
            // 거래량 급등은 현재 방향성을 증폭
            score += score > 0 ? 1 : score < 0 ? -1 : 0;
        }
        signal.setScore(score);
        signal.setSignal(score >= 2 ? "BUY" : score <= -2 ? "SELL" : "NEUTRAL");

        signal.setDescription(buildDescription(signal));
        return signal;
    }

    private double calcRsi(List<Long> closes, int period) {
        if (closes.size() <= period) return 50.0;

        List<Double> gains = new ArrayList<>();
        List<Double> losses = new ArrayList<>();
        for (int i = closes.size() - period; i < closes.size(); i++) {
            double diff = closes.get(i) - closes.get(i - 1);
            gains.add(Math.max(diff, 0));
            losses.add(Math.max(-diff, 0));
        }
        double avgGain = gains.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double avgLoss = losses.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        if (avgLoss == 0) return 100.0;
        return 100.0 - (100.0 / (1.0 + avgGain / avgLoss));
    }

    private long calcMa(List<Long> closes, int period) {
        if (closes.size() < period) return 0L;
        return (long) closes.subList(closes.size() - period, closes.size())
                            .stream().mapToLong(Long::longValue).average().orElse(0);
    }

    private String buildDescription(StockSignal s) {
        List<String> parts = new ArrayList<>();
        parts.add(String.format("RSI %.1f (%s)",
                s.getRsi(),
                s.getRsi() <= 30 ? "과매도" : s.getRsi() >= 70 ? "과매수" : "중립"));
        parts.add(String.format("5MA %s 20MA (%s)",
                s.getMa5() > s.getMa20() ? ">" : s.getMa5() < s.getMa20() ? "<" : "=",
                "BUY".equals(s.getMaSignal()) ? "상승세" : "SELL".equals(s.getMaSignal()) ? "하락세" : "보합"));
        if ("HIGH".equals(s.getVolumeSignal())) {
            parts.add(String.format("거래량 %.1f배 급등", s.getVolumeRatio()));
        }
        return String.join(" · ", parts);
    }
}
