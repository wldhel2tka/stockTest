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
public class KisTokenService {

    @Value("${kis.app-key}")
    private String appKey;

    @Value("${kis.app-secret}")
    private String appSecret;

    @Value("${kis.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate;

    private volatile String accessToken;
    private volatile LocalDateTime tokenExpiry;

    public synchronized String getAccessToken() {
        if (accessToken == null || LocalDateTime.now().isAfter(tokenExpiry.minusMinutes(10))) {
            refreshToken();
        }
        return accessToken;
    }

    @SuppressWarnings("unchecked")
    private void refreshToken() {
        String url = baseUrl + "/oauth2/tokenP";

        Map<String, String> body = new HashMap<>();
        body.put("grant_type", "client_credentials");
        body.put("appkey", appKey);
        body.put("appsecret", appSecret);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null || !responseBody.containsKey("access_token")) {
            throw new RuntimeException("KIS API 토큰 발급 실패");
        }

        accessToken = (String) responseBody.get("access_token");
        tokenExpiry = LocalDateTime.now().plusHours(23);
        log.info("KIS API 토큰 갱신 완료");
    }
}
