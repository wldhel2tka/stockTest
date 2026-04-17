package com.stocktest.service;

import com.stocktest.model.RecommendedStock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendedStockService {

    private final KisTokenService tokenService;
    private final RestTemplate restTemplate;

    @Value("${kis.app-key}")
    private String appKey;

    @Value("${kis.app-secret}")
    private String appSecret;

    @Value("${kis.base-url}")
    private String baseUrl;

    /** 거래량 상위 */
    @SuppressWarnings("unchecked")
    public List<RecommendedStock> getTopVolume(String market, long minPrice, long maxPrice, long minVolume) {
        String url = baseUrl + "/uapi/domestic-stock/v1/quotations/volume-rank"
                + "?FID_COND_MRKT_DIV_CODE=" + market
                + "&FID_COND_SCR_DIV_CODE=20171"
                + "&FID_INPUT_ISCD=0000"
                + "&FID_DIV_CLS_CODE=0"
                + "&FID_BLNG_CLS_CODE=0"
                + "&FID_TRGT_CLS_CODE=111111111"
                + "&FID_TRGT_EXLS_CLS_CODE=000000000"
                + "&FID_INPUT_PRICE_1=0"
                + "&FID_INPUT_PRICE_2=0"
                + "&FID_VOL_CNT=0"
                + "&FID_INPUT_CNT_1=0"
                + "&FID_INPUT_DATE_1=";

        HttpEntity<Void> req = new HttpEntity<>(createHeaders("FHPST01710000"));
        ResponseEntity<Map> res = restTemplate.exchange(url, HttpMethod.GET, req, Map.class);
        Map<String, Object> body = res.getBody();

        if (body == null || !"0".equals(body.get("rt_cd"))) {
            log.warn("거래량 순위 조회 실패: {}", body != null ? body.get("msg1") : "null");
            return List.of();
        }

        List<Map<String, Object>> output = (List<Map<String, Object>>) body.get("output");
        if (output == null) return List.of();

        return output.stream()
                .filter(item -> item.get("hts_kor_isnm") != null
                        && !((String) item.get("hts_kor_isnm")).isBlank())
                .map(item -> {
                    RecommendedStock s = new RecommendedStock();
                    s.setRank(parseInt(item.get("data_rank")));
                    // 거래량 순위는 mksc_shrn_iscd 사용
                    String code = getString(item, "mksc_shrn_iscd", "stck_shrn_iscd");
                    s.setCode(code);
                    s.setName(getString(item, "hts_kor_isnm"));
                    s.setCurrentPrice(parseLong(item.get("stck_prpr")));
                    s.setChange(parseLong(item.get("prdy_vrss")));
                    s.setChangeRate(parseDouble(item.get("prdy_ctrt")));
                    s.setVolume(parseLong(item.get("acml_vol")));
                    s.setTradingValue(parseLong(item.get("acml_tr_pbmn")));
                    String sign = getString(item, "prdy_vrss_sign");
                    s.setUp("1".equals(sign) || "2".equals(sign));
                    return s;
                })
                .filter(s -> (minPrice <= 0 || s.getCurrentPrice() >= minPrice)
                          && (maxPrice <= 0 || s.getCurrentPrice() <= maxPrice)
                          && (minVolume <= 0 || s.getVolume() >= minVolume))
                .limit(30)
                .collect(Collectors.toList());
    }

    /** 등락률 순위 (divCode: "0"=상승, "1"=하락) */
    @SuppressWarnings("unchecked")
    public List<RecommendedStock> getFluctuation(String market, String divCode, long minPrice, long maxPrice, long minVolume) {
        String url = baseUrl + "/uapi/domestic-stock/v1/ranking/fluctuation"
                + "?FID_COND_MRKT_DIV_CODE=" + market
                + "&FID_COND_SCR_DIV_CODE=20170"
                + "&FID_INPUT_ISCD=0000"
                + "&FID_RANK_SORT_CLS_CODE=" + divCode
                + "&FID_INPUT_CNT_1=0"
                + "&FID_PRC_CLS_CODE=0"
                + "&FID_DIV_CLS_CODE=0"
                + "&FID_BLNG_CLS_CODE=0"
                + "&FID_TRGT_CLS_CODE=111111111"
                + "&FID_TRGT_EXLS_CLS_CODE=0000000000"
                + "&FID_INPUT_PRICE_1=0"
                + "&FID_INPUT_PRICE_2=0"
                + "&FID_RST_DSPL_STND=0"
                + "&FID_MXPR_OVER_DSPL_YN=0"
                + "&FID_RSFL_RATE1=0"
                + "&FID_RSFL_RATE2=0"
                + "&FID_VOL_CNT=0"
                + "&FID_INPUT_DATE_1=";

        HttpEntity<Void> req = new HttpEntity<>(createHeaders("FHPST01700000"));
        ResponseEntity<Map> res = restTemplate.exchange(url, HttpMethod.GET, req, Map.class);
        Map<String, Object> body = res.getBody();

        if (body == null || !"0".equals(body.get("rt_cd"))) {
            log.warn("등락률 순위 조회 실패: {}", body != null ? body.get("msg1") : "null");
            return List.of();
        }

        List<Map<String, Object>> output = (List<Map<String, Object>>) body.get("output");
        if (output == null) return List.of();

        boolean gainers = "0".equals(divCode);
        return output.stream()
                .filter(item -> item.get("hts_kor_isnm") != null
                        && !((String) item.get("hts_kor_isnm")).isBlank())
                .map(item -> {
                    RecommendedStock s = new RecommendedStock();
                    s.setRank(parseInt(item.get("data_rank")));
                    String code = getString(item, "stck_shrn_iscd", "mksc_shrn_iscd");
                    s.setCode(code);
                    s.setName(getString(item, "hts_kor_isnm"));
                    s.setCurrentPrice(parseLong(item.get("stck_prpr")));
                    s.setChange(parseLong(item.get("prdy_vrss")));
                    s.setChangeRate(parseDouble(item.get("prdy_ctrt")));
                    s.setVolume(parseLong(item.get("acml_vol")));
                    s.setTradingValue(parseLong(item.get("acml_tr_pbmn")));
                    String sign = getString(item, "prdy_vrss_sign");
                    s.setUp("1".equals(sign) || "2".equals(sign));
                    return s;
                })
                .filter(s -> gainers ? s.getChangeRate() > 0 : s.getChangeRate() < 0)
                .filter(s -> (minPrice <= 0 || s.getCurrentPrice() >= minPrice)
                          && (maxPrice <= 0 || s.getCurrentPrice() <= maxPrice)
                          && (minVolume <= 0 || s.getVolume() >= minVolume))
                .sorted(gainers
                        ? Comparator.comparingDouble(RecommendedStock::getChangeRate).reversed()
                        : Comparator.comparingDouble(RecommendedStock::getChangeRate))
                .limit(30)
                .collect(Collectors.toList());
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

    private String getString(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object val = map.get(key);
            if (val != null) return val.toString();
        }
        return "";
    }

    private long parseLong(Object v) {
        if (v == null) return 0L;
        try { return Long.parseLong(v.toString().replace(",", "").trim()); }
        catch (NumberFormatException e) { return 0L; }
    }

    private double parseDouble(Object v) {
        if (v == null) return 0.0;
        try { return Double.parseDouble(v.toString().trim()); }
        catch (NumberFormatException e) { return 0.0; }
    }

    private int parseInt(Object v) {
        if (v == null) return 0;
        try { return Integer.parseInt(v.toString().trim()); }
        catch (NumberFormatException e) { return 0; }
    }
}
