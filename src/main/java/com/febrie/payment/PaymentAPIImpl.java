package com.febrie.payment;

import com.febrie.util.Logging;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * PaymentAPI 인터페이스 구현체
 */
@Slf4j
public class PaymentAPIImpl implements PaymentAPI {
    
    private final String gameServerUrl;
    private final HttpClient httpClient;
    
    public PaymentAPIImpl() {
        this("http://localhost:7998"); // 기본 게임 서버 URL
    }
    
    public PaymentAPIImpl(String gameServerUrl) {
        this.gameServerUrl = gameServerUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        log.info("결제 API 초기화 완료. 게임 서버 URL: {}", gameServerUrl);
    }

    @Override
    public boolean verifyPayment(String paymentId, double amount) {
        // 결제 검증 로직
        log.info("결제 검증: paymentId={}, amount={}", paymentId, amount);
        
        // 300원 미만은 자동 성공 처리 (프론트엔드 로직과 일치)
        if (amount < 300) {
            log.info("소액 결제 자동 성공 처리: {}", amount);
            return true;
        }
        
        // TODO: 실제 결제 검증 로직 구현 (결제 서비스 API 호출 등)
        
        return true; // 임시 구현: 항상 성공 반환
    }

    @Override
    public String processPayment(String userId, double amount, String productId) {
        // 결제 처리 로직
        String paymentId = UUID.randomUUID().toString();
        log.info("결제 처리: userId={}, amount={}, productId={}, 생성된 paymentId={}", 
                userId, amount, productId, paymentId);
                
        // TODO: 실제 결제 처리 로직 구현
                
        return paymentId;
    }

    @Override
    public Map<String, Object> handleSuccessPayment(Map<String, Object> paymentData) {
        log.info("결제 성공 처리: {}", paymentData);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 게임 서버에 결제 성공 정보 전달
            String userId = (String) paymentData.get("uid");
            int creditAmount = paymentData.containsKey("creditAmount") ? 
                    Integer.parseInt(paymentData.get("creditAmount").toString()) : 0;
            int price = paymentData.containsKey("price") ? 
                    Integer.parseInt(paymentData.get("price").toString()) : 0;
            
            // 게임 서버에 구매 정보 전송
            Map<String, Object> purchaseData = new HashMap<>();
            purchaseData.put("uid", userId);
            purchaseData.put("creditAmount", creditAmount);
            purchaseData.put("timestamp", paymentData.getOrDefault("timestamp", 
                    java.time.Instant.now().toString()));
            purchaseData.put("price", price);
            
            boolean success = sendPurchaseInfoToGameServer(purchaseData);
            
            result.put("success", success);
            result.put("paymentId", paymentData.getOrDefault("paymentKey", UUID.randomUUID().toString()));
            result.put("message", success ? "결제가 성공적으로 처리되었습니다." : "결제 처리 중 오류가 발생했습니다.");
            
            Logging.info(log, "결제 성공 처리 완료: userId={}, creditAmount={}, price={}", 
                    userId, creditAmount, price);
        } catch (Exception e) {
            log.error("결제 성공 처리 중 오류 발생: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "결제 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 게임 서버에 구매 정보를 전송합니다.
     * 
     * @param purchaseData 구매 정보
     * @return 전송 성공 여부
     */
    private boolean sendPurchaseInfoToGameServer(Map<String, Object> purchaseData) {
        try {
            // JSON 문자열로 변환
            String jsonBody = convertToJson(purchaseData);
            
            // HTTP 요청 생성
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(gameServerUrl + "/purchase"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(10))
                    .build();
            
            // 요청 전송
            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());
            
            // 응답 처리
            boolean success = response.statusCode() >= 200 && response.statusCode() < 300;
            if (success) {
                log.info("게임 서버에 구매 정보 전송 성공: {}", response.body());
            } else {
                log.error("게임 서버에 구매 정보 전송 실패: 상태 코드={}, 응답={}", 
                        response.statusCode(), response.body());
            }
            
            return success;
        } catch (IOException | InterruptedException e) {
            log.error("게임 서버에 구매 정보 전송 중 오류 발생: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Map을 JSON 문자열로 변환합니다.
     * 
     * @param map 변환할 Map
     * @return JSON 문자열
     */
    private String convertToJson(Map<String, Object> map) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                json.append(",");
            }
            first = false;
            
            json.append("\"").append(entry.getKey()).append("\":");
            
            Object value = entry.getValue();
            if (value instanceof String) {
                json.append("\"").append(value).append("\"");
            } else {
                json.append(value);
            }
        }
        
        json.append("}");
        return json.toString();
    }
}
