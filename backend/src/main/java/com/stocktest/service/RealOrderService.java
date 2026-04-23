package com.stocktest.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RealOrderService {

    @Value("${kis.app-key}")
    private String appKey;

    @Value("${kis.app-secret}")
    private String appSecret;

    @Value("${kis.base-url}")
    private String baseUrl;

    @Value("${kis.account-no}")
    private String accountNo;

    private final KisTokenService tokenService;
    private final RestTemplate restTemplate;

    // 계좌번호 앞 8자리 / 뒤 2자리
    private String cano() { return accountNo.split("-")[0]; }
    private String acntPrdtCd() { return accountNo.split("-")[1]; }

    private HttpHeaders baseHeaders(String trId) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("authorization", "Bearer " + tokenService.getAccessToken());
        h.set("appkey", appKey);
        h.set("appsecret", appSecret);
        h.set("tr_id", trId);
        h.set("custtype", "P");
        return h;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> order(String stockCode, int quantity, String side, String orderType, long price) {
        // side: BUY / SELL   orderType: MARKET / LIMIT
        String trId = side.equals("BUY") ? "TTTC0802U" : "TTTC0801U";

        Map<String, String> body = new HashMap<>();
        body.put("CANO", cano());
        body.put("ACNT_PRDT_CD", acntPrdtCd());
        body.put("PDNO", stockCode);
        body.put("ORD_DVSN", orderType.equals("MARKET") ? "01" : "00");  // 01:시장가 00:지정가
        body.put("ORD_QTY", String.valueOf(quantity));
        body.put("ORD_UNPR", orderType.equals("MARKET") ? "0" : String.valueOf(price));

        HttpEntity<Map<String, String>> req = new HttpEntity<>(body, baseHeaders(trId));
        ResponseEntity<Map> res = restTemplate.postForEntity(
                baseUrl + "/uapi/domestic-stock/v1/trading/order-cash", req, Map.class);

        Map<String, Object> resBody = res.getBody();
        log.info("KIS 주문 응답: {}", resBody);
        if (resBody == null) throw new RuntimeException("주문 응답 없음");

        String rt = (String) resBody.get("rt_cd");
        if (!"0".equals(rt)) {
            throw new RuntimeException("주문 실패: " + resBody.get("msg1"));
        }
        return resBody;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getBalance() {
        HttpHeaders h = baseHeaders("TTTC8434R");

        String url = baseUrl + "/uapi/domestic-stock/v1/trading/inquire-balance"
                + "?CANO=" + cano()
                + "&ACNT_PRDT_CD=" + acntPrdtCd()
                + "&AFHR_FLPR_YN=N&OFL_YN=&INQR_DVSN=02&UNPR_DVSN=01"
                + "&FUND_STTL_ICLD_YN=N&FNCG_AMT_AUTO_RDPT_YN=N&PRCS_DVSN=00&CTX_AREA_FK100=&CTX_AREA_NK100=";

        ResponseEntity<Map> res = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(h), Map.class);
        Map<String, Object> resBody = res.getBody();
        if (resBody == null) throw new RuntimeException("잔고 조회 응답 없음");

        String rt = (String) resBody.get("rt_cd");
        if (!"0".equals(rt)) {
            throw new RuntimeException("잔고 조회 실패: " + resBody.get("msg1"));
        }
        return resBody;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getOrderHistory() {
        HttpHeaders h = baseHeaders("TTTC8001R");

        String url = baseUrl + "/uapi/domestic-stock/v1/trading/inquire-daily-ccld"
                + "?CANO=" + cano()
                + "&ACNT_PRDT_CD=" + acntPrdtCd()
                + "&INQR_STRT_DT=20240101&INQR_END_DT=99991231"
                + "&SLL_BUY_DVSN_CD=00&INQR_DVSN=00&PDNO=&CCLD_DVSN=01"
                + "&ORD_GNO_BRNO=&ODNO=&INQR_DVSN_3=00&INQR_DVSN_1=&CTX_AREA_FK100=&CTX_AREA_NK100=";

        ResponseEntity<Map> res = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(h), Map.class);
        Map<String, Object> resBody = res.getBody();
        if (resBody == null) throw new RuntimeException("주문 내역 조회 응답 없음");

        String rt = (String) resBody.get("rt_cd");
        if (!"0".equals(rt)) {
            throw new RuntimeException("주문 내역 조회 실패: " + resBody.get("msg1"));
        }
        return (List<Map<String, Object>>) resBody.get("output1");
    }
}
