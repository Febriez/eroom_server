package com.febrie.payment.service;

import com.febrie.http.HttpClient;
import com.febrie.http.HttpMethod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * 게임 서버와 통신하는 서비스
 */
@Slf4j
public class GameServerService {

    private final String gameServerUrl;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /**
     * 게임 서버 URL을 받는 생성자
     * @param gameServerUrl 게임 서버 API URL
     */
    public GameServerService(String gameServerUrl) {
        if (gameServerUrl == null || gameServerUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("게임 서버 URL은 null이거나 비어있을 수 없습니다");
        }
        this.gameServerUrl = gameServerUrl;
    }

    /**
     * 토스페이먼츠 결제 정보를 게임 서버로 전송
     *
     * @param tossPaymentInfo 토스페이먼츠 결제 정보
     * @return 게임 서버 처리 결과
     */
    public JsonObject processPayment(@NotNull JsonObject tossPaymentInfo) {
        try {
            // 디버깅: 수신된 토스페이먼츠 결제 정보 출력
            log.info("수신된 토스페이먼츠 결제 정보:\n{}", gson.toJson(tossPaymentInfo));

            // 게임 서버에 필요한 정보 추출
            String userId = getStringOrDefault(tossPaymentInfo, "userId", "unknown");
            String creditAmount = getStringOrDefault(tossPaymentInfo, "creditAmount", "0");
            String paymentKey = getStringOrDefault(tossPaymentInfo, "paymentKey", "");
            String orderId = getStringOrDefault(tossPaymentInfo, "orderId", "");
            String amount = getStringOrDefault(tossPaymentInfo, "amount", "0");

            // 게임 서버로 전송할 데이터 구성
            JsonObject gameServerRequest = new JsonObject();
            gameServerRequest.addProperty("userId", userId);
            gameServerRequest.addProperty("creditAmount", creditAmount);
            gameServerRequest.addProperty("paymentKey", paymentKey);
            gameServerRequest.addProperty("orderId", orderId);
            gameServerRequest.addProperty("amount", amount);
            gameServerRequest.addProperty("paymentMethod", "toss");
            gameServerRequest.addProperty("timestamp", System.currentTimeMillis());

            // 디버깅: 게임 서버로 전송될 데이터 출력
            log.info("게임 서버로 전송될 데이터:\n{}", gson.toJson(gameServerRequest));

            // 디버깅 모드: 실제 전송하지 않고 가상 응답 반환
            boolean isDebugMode = true; // 디버깅 모드 활성화

            if (isDebugMode) {
                log.info("디버깅 모드: 게임 서버 API 호출 생략");

                // 가상 응답 생성
                JsonObject debugResponse = new JsonObject();
                debugResponse.addProperty("success", true);
                debugResponse.addProperty("message", "디버깅 모드: 크레딧 추가 처리 완료");
                debugResponse.addProperty("userId", userId);
                debugResponse.addProperty("creditAdded", creditAmount);
                debugResponse.addProperty("newCreditBalance", Integer.parseInt(creditAmount) + 1000); // 가상의 잔액
                debugResponse.addProperty("timestamp", System.currentTimeMillis());

                log.info("디버깅 모드 가상 응답:\n{}", gson.toJson(debugResponse));
                return debugResponse;
            }

            // 실제 환경: 게임 서버 API 호출
            try {
                log.info("게임 서버 API 호출: {}", gameServerUrl + "/api/payment/credit");

                // 헤더 설정
                JsonObject headers = new JsonObject();
                headers.addProperty("Content-Type", "application/json");
                headers.addProperty("X-API-KEY", "GAME_SERVER_API_KEY"); // 게임 서버 API 키

                // API 호출
                HttpClient client = new HttpClient(gameServerUrl + "/api/payment/credit");
                JsonObject response = client.sendRequest(HttpMethod.POST, headers, gameServerRequest);

                log.info("게임 서버 응답:\n{}", gson.toJson(response));
                return response;
            } catch (IOException e) {
                log.error("게임 서버 API 호출 중 오류: {}", e.getMessage(), e);

                JsonObject errorResponse = new JsonObject();
                errorResponse.addProperty("success", false);
                errorResponse.addProperty("error", "COMMUNICATION_ERROR");
                errorResponse.addProperty("message", "게임 서버 통신 중 오류: " + e.getMessage());
                return errorResponse;
            }

        } catch (Exception e) {
            log.error("결제 정보 처리 중 오류: {}", e.getMessage(), e);

            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("success", false);
            errorResponse.addProperty("error", "PROCESSING_ERROR");
            errorResponse.addProperty("message", "결제 정보 처리 중 오류: " + e.getMessage());
            return errorResponse;
        }
    }

    /**
     * JsonObject에서 문자열 값을 안전하게 가져오는 유틸리티 메소드
     */
    private String getStringOrDefault(@NotNull JsonObject json, String key, String defaultValue) {
        if (json.has(key) && !json.get(key).isJsonNull()) {
            return json.get(key).getAsString();
        }
        return defaultValue;
    }
}