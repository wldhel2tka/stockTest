package com.stocktest.service;

import com.stocktest.model.ChartCandle;
import com.stocktest.model.StockPrice;
import com.stocktest.model.StockSearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockService {

    private final KisTokenService tokenService;
    private final RestTemplate restTemplate;

    @Value("${kis.app-key}")
    private String appKey;

    @Value("${kis.app-secret}")
    private String appSecret;

    @Value("${kis.base-url}")
    private String baseUrl;

    private static final List<StockSearchResult> STOCK_LIST = List.of(
            // KOSPI
            new StockSearchResult("005930", "삼성전자", "KOSPI"),
            new StockSearchResult("005935", "삼성전자우", "KOSPI"),
            new StockSearchResult("000660", "SK하이닉스", "KOSPI"),
            new StockSearchResult("373220", "LG에너지솔루션", "KOSPI"),
            new StockSearchResult("207940", "삼성바이오로직스", "KOSPI"),
            new StockSearchResult("005380", "현대차", "KOSPI"),
            new StockSearchResult("068270", "셀트리온", "KOSPI"),
            new StockSearchResult("005490", "POSCO홀딩스", "KOSPI"),
            new StockSearchResult("035420", "NAVER", "KOSPI"),
            new StockSearchResult("006400", "삼성SDI", "KOSPI"),
            new StockSearchResult("035720", "카카오", "KOSPI"),
            new StockSearchResult("051910", "LG화학", "KOSPI"),
            new StockSearchResult("000270", "기아", "KOSPI"),
            new StockSearchResult("105560", "KB금융", "KOSPI"),
            new StockSearchResult("055550", "신한지주", "KOSPI"),
            new StockSearchResult("012330", "현대모비스", "KOSPI"),
            new StockSearchResult("028260", "삼성물산", "KOSPI"),
            new StockSearchResult("003550", "LG", "KOSPI"),
            new StockSearchResult("086790", "하나금융지주", "KOSPI"),
            new StockSearchResult("034730", "SK", "KOSPI"),
            new StockSearchResult("316140", "우리금융지주", "KOSPI"),
            new StockSearchResult("015760", "한국전력", "KOSPI"),
            new StockSearchResult("323410", "카카오뱅크", "KOSPI"),
            new StockSearchResult("259960", "크래프톤", "KOSPI"),
            new StockSearchResult("329180", "HD현대중공업", "KOSPI"),
            new StockSearchResult("011200", "HMM", "KOSPI"),
            new StockSearchResult("034020", "두산에너빌리티", "KOSPI"),
            new StockSearchResult("003670", "포스코퓨처엠", "KOSPI"),
            new StockSearchResult("018260", "삼성에스디에스", "KOSPI"),
            new StockSearchResult("009150", "삼성전기", "KOSPI"),
            new StockSearchResult("028050", "삼성중공업", "KOSPI"),
            new StockSearchResult("010130", "고려아연", "KOSPI"),
            new StockSearchResult("004020", "현대제철", "KOSPI"),
            new StockSearchResult("000810", "삼성화재", "KOSPI"),
            new StockSearchResult("090430", "아모레퍼시픽", "KOSPI"),
            new StockSearchResult("033780", "KT&G", "KOSPI"),
            new StockSearchResult("011070", "LG이노텍", "KOSPI"),
            new StockSearchResult("024110", "기업은행", "KOSPI"),
            new StockSearchResult("030200", "KT", "KOSPI"),
            new StockSearchResult("017670", "SK텔레콤", "KOSPI"),
            new StockSearchResult("032640", "LGU+", "KOSPI"),
            new StockSearchResult("000720", "현대건설", "KOSPI"),
            new StockSearchResult("096770", "SK이노베이션", "KOSPI"),
            new StockSearchResult("047050", "포스코인터내셔널", "KOSPI"),
            new StockSearchResult("161390", "한국타이어앤테크놀로지", "KOSPI"),
            new StockSearchResult("036460", "한국가스공사", "KOSPI"),
            new StockSearchResult("000100", "유한양행", "KOSPI"),
            new StockSearchResult("009540", "HD한국조선해양", "KOSPI"),
            new StockSearchResult("010950", "S-Oil", "KOSPI"),
            new StockSearchResult("002380", "KCC", "KOSPI"),
            // KOSDAQ
            new StockSearchResult("247540", "에코프로비엠", "KOSDAQ"),
            new StockSearchResult("086520", "에코프로", "KOSDAQ"),
            new StockSearchResult("091990", "셀트리온헬스케어", "KOSDAQ"),
            new StockSearchResult("196170", "알테오젠", "KOSDAQ"),
            new StockSearchResult("293490", "카카오게임즈", "KOSDAQ"),
            new StockSearchResult("122870", "와이지엔터테인먼트", "KOSDAQ"),
            new StockSearchResult("035760", "CJ ENM", "KOSDAQ"),
            new StockSearchResult("214150", "클래시스", "KOSDAQ"),
            new StockSearchResult("352820", "하이브", "KOSDAQ"),
            new StockSearchResult("263750", "펄어비스", "KOSDAQ"),
            new StockSearchResult("145020", "휴젤", "KOSDAQ"),
            new StockSearchResult("041510", "에스엠", "KOSDAQ"),
            new StockSearchResult("035900", "JYP Ent.", "KOSDAQ"),
            new StockSearchResult("357780", "솔브레인", "KOSDAQ"),
            new StockSearchResult("064260", "다날", "KOSDAQ"),
            new StockSearchResult("095660", "네오위즈", "KOSDAQ"),
            new StockSearchResult("251270", "넷마블", "KOSDAQ")
    );

    public List<StockSearchResult> search(String query) {
        String q = query.trim();
        if (q.matches("\\d{6}")) {
            return STOCK_LIST.stream()
                    .filter(s -> s.getCode().equals(q))
                    .collect(Collectors.toList());
        }
        String lowerQ = q.toLowerCase();
        return STOCK_LIST.stream()
                .filter(s -> s.getName().toLowerCase().contains(lowerQ) ||
                             s.getCode().startsWith(q))
                .limit(10)
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public StockPrice getPrice(String code) {
        String url = baseUrl + "/uapi/domestic-stock/v1/quotations/inquire-price" +
                "?FID_COND_MRKT_DIV_CODE=J&FID_INPUT_ISCD=" + code;

        HttpEntity<Void> request = new HttpEntity<>(createHeaders("FHKST01010100"));
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);

        Map<String, Object> body = response.getBody();
        if (body == null || !"0".equals(body.get("rt_cd"))) {
            throw new RuntimeException("주가 조회 실패: " + (body != null ? body.get("msg1") : "응답 없음"));
        }

        Map<String, Object> output = (Map<String, Object>) body.get("output");

        StockPrice price = new StockPrice();
        price.setCode(code);
        price.setName((String) output.get("hts_kor_isnm"));
        price.setCurrentPrice(parseLong(output.get("stck_prpr")));
        price.setChange(parseLong(output.get("prdy_vrss")));
        price.setChangeRate(parseDouble(output.get("prdy_ctrt")));
        price.setOpenPrice(parseLong(output.get("stck_oprc")));
        price.setHighPrice(parseLong(output.get("stck_hgpr")));
        price.setLowPrice(parseLong(output.get("stck_lwpr")));
        price.setVolume(parseLong(output.get("acml_vol")));

        String sign = (String) output.get("prdy_vrss_sign");
        price.setUp("1".equals(sign) || "2".equals(sign));

        return price;
    }

    @SuppressWarnings("unchecked")
    public List<ChartCandle> getChart(String code, String period, String startDate, String endDate) {
        String url = baseUrl + "/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice" +
                "?FID_COND_MRKT_DIV_CODE=J" +
                "&FID_INPUT_ISCD=" + code +
                "&FID_INPUT_DATE_1=" + startDate +
                "&FID_INPUT_DATE_2=" + endDate +
                "&FID_PERIOD_DIV_CODE=" + period +
                "&FID_ORG_ADJ_PRC=0";

        HttpEntity<Void> request = new HttpEntity<>(createHeaders("FHKST03010100"));
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);

        Map<String, Object> body = response.getBody();
        if (body == null || !"0".equals(body.get("rt_cd"))) {
            throw new RuntimeException("차트 조회 실패: " + (body != null ? body.get("msg1") : "응답 없음"));
        }

        List<Map<String, Object>> output2 = (List<Map<String, Object>>) body.get("output2");
        if (output2 == null) return List.of();

        return output2.stream()
                .filter(item -> {
                    Object clpr = item.get("stck_clpr");
                    return clpr != null && !((String) clpr).isEmpty() && !"0".equals(clpr);
                })
                .map(item -> {
                    String dateStr = (String) item.get("stck_bsop_date");
                    ChartCandle candle = new ChartCandle();
                    candle.setDate(dateStr.substring(0, 4) + "-" +
                                   dateStr.substring(4, 6) + "-" +
                                   dateStr.substring(6, 8));
                    candle.setOpen(parseLong(item.get("stck_oprc")));
                    candle.setHigh(parseLong(item.get("stck_hgpr")));
                    candle.setLow(parseLong(item.get("stck_lwpr")));
                    candle.setClose(parseLong(item.get("stck_clpr")));
                    candle.setVolume(parseLong(item.get("acml_vol")));
                    return candle;
                })
                .sorted(Comparator.comparing(ChartCandle::getDate))
                .collect(Collectors.toList());
    }

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * 분봉 차트 조회 (KIS FHKST03010200)
     * type: "1H" = 최근 1시간, "1D" = 오늘 전체
     */
    @SuppressWarnings("unchecked")
    public List<ChartCandle> getMinuteChart(String code, String type) {
        ZonedDateTime now = ZonedDateTime.now(KST);
        ZonedDateTime cutoff = type.equals("1H")
                ? now.minusHours(1)
                : now.toLocalDate().atStartOfDay(KST);

        int maxPages = type.equals("1H") ? 3 : 20;
        List<ChartCandle> result = new ArrayList<>();
        String queryTime = now.format(DateTimeFormatter.ofPattern("HHmmss"));

        for (int page = 0; page < maxPages; page++) {
            List<ChartCandle> batch = fetchMinuteBatch(code, queryTime);
            if (batch.isEmpty()) break;

            result.addAll(batch);

            ChartCandle oldest = batch.get(batch.size() - 1);
            ZonedDateTime oldestTime = ZonedDateTime.ofInstant(
                    Instant.ofEpochSecond(oldest.getTimestamp()), KST);

            if (!oldestTime.isAfter(cutoff)) break;

            // 다음 페이지: 가장 오래된 시간의 1초 전
            queryTime = oldestTime.minusSeconds(1).format(DateTimeFormatter.ofPattern("HHmmss"));
        }

        return result.stream()
                .filter(c -> c.getTimestamp() != null &&
                             ZonedDateTime.ofInstant(Instant.ofEpochSecond(c.getTimestamp()), KST).isAfter(cutoff))
                .sorted(Comparator.comparing(ChartCandle::getTimestamp))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private List<ChartCandle> fetchMinuteBatch(String code, String time) {
        String url = baseUrl + "/uapi/domestic-stock/v1/quotations/inquire-time-itemchartprice" +
                "?FID_ETC_CLS_CODE=" +
                "&FID_COND_MRKT_DIV_CODE=J" +
                "&FID_INPUT_ISCD=" + code +
                "&FID_INPUT_HOUR_1=" + time +
                "&FID_PW_DATA_INCU_YN=Y";

        try {
            HttpEntity<Void> request = new HttpEntity<>(createHeaders("FHKST03010200"));
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);

            Map<String, Object> body = response.getBody();
            if (body == null || !"0".equals(body.get("rt_cd"))) return List.of();

            List<Map<String, Object>> output2 = (List<Map<String, Object>>) body.get("output2");
            if (output2 == null) return List.of();

            return output2.stream()
                    .filter(item -> {
                        Object vol = item.get("cntg_vol");
                        return vol != null && !"0".equals(vol.toString().trim());
                    })
                    .map(item -> {
                        String dateStr = (String) item.get("stck_bsop_date");
                        String timeStr = (String) item.get("stck_cntg_hour");
                        LocalDateTime ldt = LocalDateTime.parse(dateStr + timeStr, DT_FMT);
                        long ts = ldt.atZone(KST).toEpochSecond();

                        ChartCandle c = new ChartCandle();
                        c.setTimestamp(ts);
                        c.setOpen(parseLong(item.get("stck_oprc")));
                        c.setHigh(parseLong(item.get("stck_hgpr")));
                        c.setLow(parseLong(item.get("stck_lwpr")));
                        c.setClose(parseLong(item.get("stck_prpr")));
                        c.setVolume(parseLong(item.get("cntg_vol")));
                        return c;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("분봉 배치 조회 실패 (time={}): {}", time, e.getMessage());
            return List.of();
        }
    }

    private HttpHeaders createHeaders(String trId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("authorization", "Bearer " + tokenService.getAccessToken());
        headers.set("appkey", appKey);
        headers.set("appsecret", appSecret);
        headers.set("tr_id", trId);
        headers.set("custtype", "P");
        return headers;
    }

    private long parseLong(Object value) {
        if (value == null) return 0L;
        try {
            String str = value.toString().replace(",", "").trim();
            if (str.isEmpty()) return 0L;
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private double parseDouble(Object value) {
        if (value == null) return 0.0;
        try {
            String str = value.toString().trim();
            if (str.isEmpty()) return 0.0;
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
