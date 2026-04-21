package com.stocktest.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KisApprovalKeyService {

    @Value("${kis.app-key}")
    private String appKey;

    @Value("${kis.app-secret}")
    private String appSecret;

    @Value("${kis.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate;

    private volatile String approvalKey;
    private volatile LocalDateTime keyExpiry;

    public synchronized String getApprovalKey() {
        if (approvalKey == null || LocalDateTime.now().isAfter(keyExpiry.minusHours(1))) {
            refreshApprovalKey();
        }
        return approvalKey;
    }

    @SuppressWarnings("unchecked")
    private void refreshApprovalKey() {
        String url = baseUrl + "/oauth2/Approval";

        Map<String, String> body = new HashMap<>();
        body.put("grant_type", "client_credentials");
        body.put("appkey", appKey);
        body.put("secretkey", appSecret);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null || !responseBody.containsKey("approval_key")) {
            throw new RuntimeException("KIS WebSocket 승인키 발급 실패");
        }

        approvalKey = (String) responseBody.get("approval_key");
        keyExpiry = LocalDateTime.now().plusDays(1);
        log.info("KIS WebSocket 승인키 발급 완료");
    }
}
