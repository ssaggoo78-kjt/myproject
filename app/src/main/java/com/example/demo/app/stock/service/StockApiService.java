package com.example.demo.app.stock.service;

import com.example.demo.app.stock.dto.StockOrderResponse;
import com.example.demo.app.stock.dto.TokenResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class StockApiService {

    private static final Logger logger = LoggerFactory.getLogger(StockApiService.class);

    @Value("${korea.investment.app.key}")
    private String appKey;

    @Value("${korea.investment.app.secret}")
    private String appSecret;

    @Value("${korea.investment.account.number}")
    private String accountNumber;

    private String accessToken;
    private int expiresIn;
    private long tokenIssuedAt;

    private static final String KOREA_INVESTMENT_API_DOMAIN = "https://openapivts.koreainvestment.com:29443";

    private String[] getAccountParts() {
        return accountNumber.split("-");
    }

    private void issueToken() {
        logger.info("Issuing new access token...");
        String url = KOREA_INVESTMENT_API_DOMAIN + "/oauth2/tokenP";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = new HashMap<>();
        body.put("grant_type", "client_credentials");
        body.put("appkey", appKey);
        body.put("appsecret", appSecret);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
        RestTemplate restTemplate = new RestTemplate();
        TokenResponse tokenResponse = restTemplate.postForObject(url, request, TokenResponse.class);

        if (tokenResponse == null || tokenResponse.getAccess_token() == null) {
            throw new RuntimeException("Failed to issue access token");
        }

        this.accessToken = tokenResponse.getAccess_token();
        this.expiresIn = tokenResponse.getExpires_in();
        this.tokenIssuedAt = System.currentTimeMillis();
        logger.info("Access token issued successfully.");
    }

    private boolean isTokenExpired() {
        if (accessToken == null) {
            return true;
        }
        long buffer = 60 * 1000; // 60 seconds buffer
        return (System.currentTimeMillis() - tokenIssuedAt) >= (expiresIn * 1000 - buffer);
    }

    private synchronized String getValidAccessToken() {
        if (isTokenExpired()) {
            issueToken();
        }
        return accessToken;
    }

    public StockOrderResponse orderStock(String stockCode, int quantity) {
        String url = KOREA_INVESTMENT_API_DOMAIN + "/uapi/domestic-stock/v1/trading/order-cash";

        String[] account = getAccountParts();

        Map<String, String> bodyMap = new TreeMap<>();
        bodyMap.put("CANO", account[0]);
        bodyMap.put("ACNT_PRDT_CD", account[1]);
        bodyMap.put("PDNO", stockCode);
        bodyMap.put("ORD_DVSN", "01"); // 지정가
        bodyMap.put("ORD_QTY", String.valueOf(quantity));
        bodyMap.put("ORD_UNPR", "0"); 

        String jsonBody;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            jsonBody = objectMapper.writeValueAsString(bodyMap);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize orderStock body", e);
            throw new RuntimeException(e);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("application/json; charset=UTF-8"));
        headers.set("Authorization", "Bearer " + getValidAccessToken());
        headers.set("appkey", appKey);
        headers.set("appsecret", appSecret);
        headers.set("tr_id", "VTTC0802U"); // 가상 매수
        headers.set("custtype", "P");

        HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<StockOrderResponse> response = restTemplate.exchange(url, HttpMethod.POST, request, StockOrderResponse.class);
        logger.info("Order stock response: {}", response.getBody());
        return response.getBody();
    }
    
    public StockOrderResponse sellStock(String stockCode, int quantity) {
        String url = KOREA_INVESTMENT_API_DOMAIN + "/uapi/domestic-stock/v1/trading/order-cash";
    
        String[] account = getAccountParts();
    
        Map<String, String> bodyMap = new TreeMap<>();
        bodyMap.put("CANO", account[0]);
        bodyMap.put("ACNT_PRDT_CD", account[1]);
        bodyMap.put("PDNO", stockCode);
        bodyMap.put("ORD_DVSN", "01"); // 지정가 매도
        bodyMap.put("ORD_QTY", String.valueOf(quantity));
        bodyMap.put("ORD_UNPR", "0");
    
        String jsonBody;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            jsonBody = objectMapper.writeValueAsString(bodyMap);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize sellStock body", e);
            throw new RuntimeException(e);
        }
    
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("application/json; charset=UTF-8"));
        headers.set("Authorization", "Bearer " + getValidAccessToken());
        headers.set("appkey", appKey);
        headers.set("appsecret", appSecret);
        headers.set("tr_id", "VTTC0801U"); // 가상 매도
        headers.set("custtype", "P");
    
        HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<StockOrderResponse> response = restTemplate.exchange(url, HttpMethod.POST, request, StockOrderResponse.class);
        logger.info("Sell stock response: {}", response.getBody());
        return response.getBody();
    }


    public Map<String, Object> getMyStocks() {
        logger.info("보유 주식 및 계좌 정보 조회 API 호출 시작...");
        String url = KOREA_INVESTMENT_API_DOMAIN + "/uapi/domestic-stock/v1/trading/inquire-balance";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + getValidAccessToken());
        headers.set("appkey", appKey);
        headers.set("appsecret", appSecret);
        headers.set("tr_id", "VTTC8434R");

        String[] account = getAccountParts();
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("CANO", account[0])
                .queryParam("ACNT_PRDT_CD", account[1])
                .queryParam("AFHR_FLPR_YN", "N")
                .queryParam("OFL_YN", "")
                .queryParam("INQR_DVSN", "01")
                .queryParam("UNPR_DVSN", "01")
                .queryParam("FUND_STTL_ICLD_YN", "N")
                .queryParam("FNCG_AMT_AUTO_RDPT_YN", "N")
                .queryParam("PRCS_DVSN", "00")
                .queryParam("CTX_AREA_FK100", "")
                .queryParam("CTX_AREA_NK100", "");

        HttpEntity<?> entity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<String> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                entity,
                String.class);

        logger.info("Get my stocks raw response: {}", response.getBody());

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> responseMap;
        try {
            responseMap = objectMapper.readValue(response.getBody(), new TypeReference<Map<String, Object>>() {});
            if (!"0".equals(responseMap.get("rt_cd"))) {
                String errorMsg = (String) responseMap.get("msg1");
                logger.error("API Error while fetching my stocks: {}", errorMsg);
                throw new RuntimeException("API Error: " + errorMsg);
            }
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse getMyStocks response", e);
            throw new RuntimeException("Failed to parse API response", e);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("stocks", responseMap.get("output1"));
        if (responseMap.containsKey("output2") && responseMap.get("output2") instanceof List) {
            List<?> output2List = (List<?>) responseMap.get("output2");
            if (!output2List.isEmpty()) {
                result.put("accountSummary", output2List.get(0));
            }
        }
        result.put("accountNumber", accountNumber);

        logger.info("Processed account info: {}", result);
        return result;
    }

    public Map<String, Object> getStockDetails(String stockCode) {
        logger.info("종목 상세 정보(가격) 조회 API 호출 시작... 종목 코드: {}", stockCode);
        String url = KOREA_INVESTMENT_API_DOMAIN + "/uapi/domestic-stock/v1/quotations/inquire-price";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + getValidAccessToken());
        headers.set("appkey", appKey);
        headers.set("appsecret", appSecret);
        headers.set("tr_id", "FHKST01010100");

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                .queryParam("FID_INPUT_ISCD", stockCode);

        HttpEntity<?> entity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<String> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                entity,
                String.class);

        logger.info("Get stock details raw response for {}: {}", stockCode, response.getBody());

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> responseMap;
        try {
            responseMap = objectMapper.readValue(response.getBody(), new TypeReference<Map<String, Object>>() {});
            if (!"0".equals(responseMap.get("rt_cd"))) {
                String errorMsg = (String) responseMap.get("msg1");
                logger.error("API Error for stock {}: {}", stockCode, errorMsg);
                return null;
            }
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse getStockDetails response", e);
            return null;
        }

        Map<String, Object> result = new HashMap<>();
        if (responseMap.containsKey("output")) {
            Object outputObj = responseMap.get("output");
            if (outputObj instanceof Map) {
                Map<String, String> output = (Map<String, String>) outputObj;
                String price = output.get("stck_prpr");
                if (price == null || price.trim().isEmpty()) {
                    logger.warn("Price not found in API response for stock: {}", stockCode);
                    return null;
                }
                result.put("price", price);
                result.put("code", stockCode);
            } else {
                 logger.warn("Unexpected 'output' format for stock code: {}", stockCode);
                 return null;
            }
        } else {
             logger.warn("No 'output' field in API response for stock: {}", stockCode);
             return null;
        }

        return result;
    }

    public Map<String, Object> getTopTradedStocks() {
        logger.info("거래량 상위 종목 조회 API 호출 시작...");
        String url = KOREA_INVESTMENT_API_DOMAIN + "/uapi/domestic-stock/v1/quotations/volume-rank";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + getValidAccessToken());
        headers.set("appkey", appKey);
        headers.set("appsecret", appSecret);
        headers.set("tr_id", "VHPST01710000");

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
            .queryParam("FID_COND_MRKT_DIV_CODE", "J")
            .queryParam("FID_COND_SCR_DIV_CODE", "20171")
            .queryParam("FID_INPUT_ISCD", "0000")
            .queryParam("FID_DIV_CLS_CODE", "0")
            .queryParam("FID_BLNG_CLS_CODE", "0");

        HttpEntity<?> entity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<String> response = restTemplate.exchange(
            builder.toUriString(),
            HttpMethod.GET,
            entity,
            String.class);

        logger.info("Get top traded stocks raw response: {}", response.getBody());

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> responseMap;
        try {
            responseMap = objectMapper.readValue(response.getBody(), new TypeReference<Map<String, Object>>() {});
            if (!"0".equals(responseMap.get("rt_cd"))) {
                String errorMsg = (String) responseMap.get("msg1");
                logger.error("API Error while fetching top traded stocks: {}", errorMsg);
                throw new RuntimeException("API Error: " + errorMsg);
            }
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse getTopTradedStocks response", e);
            throw new RuntimeException("Failed to parse API response", e);
        }

        return responseMap;
    }



}