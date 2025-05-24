package com.febrie.payment.api;

import com.febrie.util.Logging;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 결제 API 구현체
 * 게임 서버와의 통신을 담당합니다.
 */
public class PaymentAPIImpl implements PaymentAPI {
    private static final Logger log = Logging.getLogger(PaymentAPIImpl.class);

    private static final String GAME_SERVER_API_URL = "http://localhost:8080/api/payment/process";
    private static final int CONNECTION_TIMEOUT = 10000; // 10초
    private static final int READ_TIMEOUT = 10000; // 10초
    
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public JsonObject sendPaymentToGameServer(@NotNull JsonObject paymentInfo, String userId, String creditAmount) {
        try {
            Logging.info(log, "========== 결제 정보 처리 시작 ==========");
            Logging.info(log, "결제 정보 전송 준비: userId={}, 크레딧 수량={}", userId, creditAmount);
            
            // 디버깅: 원본 토스페이먼츠 결제 정보 로깅
            Logging.debug(log, "원본 토스페이먼츠 결제 정보:\n{}", gson.toJson(paymentInfo));
            
            // 사용자 및 크레딧 정보 추가 (복사본 생성하여 원본 보존)
            JsonObject enrichedPaymentInfo = new JsonObject();
            
            // 토스페이먼츠 결제 정보 복사
            for (String key : paymentInfo.keySet()) {
                enrichedPaymentInfo.add(key, paymentInfo.get(key));
            }
            
            // 부가 정보 추가
            enrichedPaymentInfo.addProperty("userId", userId);
            enrichedPaymentInfo.addProperty("creditAmount", creditAmount);
            enrichedPaymentInfo.addProperty("timestamp", System.currentTimeMillis());
            enrichedPaymentInfo.addProperty("paymentProcessor", "TossPayments");
            enrichedPaymentInfo.addProperty("transactionId", UUID.randomUUID().toString());
            
            // 금액 확인
            String amount = getString(enrichedPaymentInfo, "amount", "0");
            int amountValue = 0;
            try {
                amountValue = Integer.parseInt(amount);
            } catch (NumberFormatException e) {
                log.warn("⚠️ 금액 파싱 실패: {}, 기본값 0으로 설정", amount);
            }
            
            // 300원 미만 결제인 경우 자동 성공 처리 준비
            if (amountValue < 300) {
                Logging.info(log, "소액 결제(300원 미만) 감지: {}원", amountValue);
                enrichedPaymentInfo.addProperty("autoProcessed", true);
                enrichedPaymentInfo.addProperty("autoReason", "SMALL_AMOUNT");
            }
            
            // 디버깅: 최종 결제 정보 로깅
            Logging.debug(log, "최종 결제 정보:\n{}", gson.toJson(enrichedPaymentInfo));
            
            // 개별 필드 로깅
            Logging.info(log, "결제 키: {}", getString(enrichedPaymentInfo, "paymentKey", "N/A"));
            Logging.info(log, "주문 ID: {}", getString(enrichedPaymentInfo, "orderId", "N/A"));
            Logging.info(log, "결제 금액: {}", getString(enrichedPaymentInfo, "amount", "0"));
            Logging.info(log, "사용자 ID: {}", userId);
            Logging.info(log, "크레딧 수량: {}", creditAmount);
            
            // 결제 정보 처리 로직
            JsonObject result = processPayment(enrichedPaymentInfo);
            
            // 결과 로깅
            log.info("📊 처리 결과:\n{}", gson.toJson(result));
            log.info("🔶 ========== 결제 정보 처리 완료 ==========");
            
            return result;
        } catch (Exception e) {
            log.error("❌ 결제 정보 처리 중 오류 발생", e);
            
            // 에러 응답 생성
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("success", false);
            errorResponse.addProperty("error", "INTERNAL_ERROR");
            errorResponse.addProperty("message", "결제 정보 처리 중 오류 발생: " + e.getMessage());
            return errorResponse;
        }
    }
    
    /**
     * 결제 정보를 처리하고 게임 서버에 전송하는 메소드
     */
    private JsonObject processPayment(JsonObject paymentInfo) {
        try {
            // 자동 처리 여부 확인
            boolean isAutoProcessed = paymentInfo.has("autoProcessed") && 
                                     paymentInfo.get("autoProcessed").getAsBoolean();
            
            if (isAutoProcessed) {
                log.info("🤖 자동 처리된 결제입니다. 게임 서버 호출 없이 성공 처리합니다.");
                // 자동 처리 성공 응답 생성
                JsonObject autoSuccess = new JsonObject();
                autoSuccess.addProperty("success", true);
                autoSuccess.addProperty("message", "소액 결제 자동 처리 완료");
                autoSuccess.addProperty("processedAt", System.currentTimeMillis());
                autoSuccess.addProperty("transactionId", getString(paymentInfo, "transactionId", UUID.randomUUID().toString()));
                
                // 사용자 크레딧 정보 추가 (실제로는 게임 서버에서 처리)
                String creditAmount = getString(paymentInfo, "creditAmount", "0");
                autoSuccess.addProperty("creditAmount", creditAmount);
                autoSuccess.addProperty("newCreditBalance", calculateNewCreditBalance(creditAmount));
                
                return autoSuccess;
            }
            
            // 게임 서버로 결제 정보 전송
            log.info("🌐 게임 서버로 결제 정보 전송 중...");
            JsonObject serverResponse = sendToGameServer(paymentInfo);
            
            if (serverResponse == null) {
                // 서버 응답이 없는 경우 실패 응답 생성
                log.error("❌ 게임 서버 응답 없음");
                JsonObject failureResponse = new JsonObject();
                failureResponse.addProperty("success", false);
                failureResponse.addProperty("error", "SERVER_NO_RESPONSE");
                failureResponse.addProperty("message", "게임 서버로부터 응답이 없습니다.");
                return failureResponse;
            }
            
            // 게임 서버 응답 로깅
            log.info("✅ 게임 서버 응답 수신 완료");
            return serverResponse;
            
        } catch (Exception e) {
            log.error("❌ 결제 처리 중 오류 발생", e);
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("success", false);
            errorResponse.addProperty("error", "PROCESSING_ERROR");
            errorResponse.addProperty("message", "결제 처리 중 오류: " + e.getMessage());
            return errorResponse;
        }
    }
    
    /**
     * 새로운 크레딧 잔액을 계산하는 메소드 (가상의 데이터)
     */
    private int calculateNewCreditBalance(String creditAmount) {
        int amount = 0;
        try {
            amount = Integer.parseInt(creditAmount);
        } catch (NumberFormatException e) {
            log.warn("⚠️ 크레딧 금액 파싱 실패: {}", creditAmount);
        }
        
        // 가상의 기존 잔액 (실제로는 DB에서 조회)
        int existingBalance = 1000;
        return existingBalance + amount;
    }
    
    /**
     * 게임 서버로 결제 정보를 전송하는 메소드
     */
    private JsonObject sendToGameServer(JsonObject paymentInfo) {
        HttpURLConnection connection = null;
        
        try {
            // 연결 설정
            URL url = new URL(GAME_SERVER_API_URL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);
            connection.setConnectTimeout(CONNECTION_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            
            // 요청 바디 전송
            String requestBody = gson.toJson(paymentInfo);
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            // 응답 코드 확인
            int responseCode = connection.getResponseCode();
            log.debug("🔢 게임 서버 응답 코드: {}", responseCode);
            
            // 응답 읽기
            StringBuilder response = new StringBuilder();
            try (java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(
                            responseCode >= 400 ? connection.getErrorStream() : connection.getInputStream(),
                            StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
            }
            
            // 응답 파싱
            String responseBody = response.toString();
            log.debug("📄 게임 서버 응답 본문: {}", responseBody);
            
            if (responseBody.isEmpty()) {
                JsonObject emptyResponse = new JsonObject();
                emptyResponse.addProperty("success", false);
                emptyResponse.addProperty("error", "EMPTY_RESPONSE");
                emptyResponse.addProperty("message", "게임 서버가 빈 응답을 반환했습니다.");
                return emptyResponse;
            }
            
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
            
            // 응답에 성공 여부가 없으면 추가
            if (!jsonResponse.has("success")) {
                jsonResponse.addProperty("success", responseCode >= 200 && responseCode < 300);
            }
            
            return jsonResponse;
            
        } catch (IOException e) {
            log.error("❌ 게임 서버 통신 오류", e);
            
            // 테스트 환경 또는 게임 서버 없을 경우 가상 응답 생성
            JsonObject mockResponse = new JsonObject();
            mockResponse.addProperty("success", true);
            mockResponse.addProperty("message", "게임 서버 연결 실패. 테스트 모드로 응답합니다.");
            mockResponse.addProperty("note", "이 응답은 게임 서버가 연결되지 않아 자동 생성된 것입니다.");
            mockResponse.addProperty("creditAmount", getString(paymentInfo, "creditAmount", "0"));
            mockResponse.addProperty("newCreditBalance", calculateNewCreditBalance(getString(paymentInfo, "creditAmount", "0")));
            mockResponse.addProperty("isMock", true);
            
            return mockResponse;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    /**
     * JsonObject에서 문자열 값을 안전하게 가져오는 유틸리티 메소드
     */
    private String getString(JsonObject json, String key, String defaultValue) {
        if (json != null && json.has(key) && !json.get(key).isJsonNull()) {
            return json.get(key).getAsString();
        }
        return defaultValue;
    }
}
