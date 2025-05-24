package com.febrie.payment.controller;

import com.febrie.payment.api.PaymentAPI;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class PaymentController implements HttpHandler {

    private static final String TOSS_API_BASE_URL = "https://api.tosspayments.com/v2/payments/";
    private static final String TOSS_SECRET_KEY = "test_sk_mBZ1gQ4YVX55qxM2MZaXrl2KPoqN"; // 테스트 시크릿 키, 실제 운영 시 교체 필요

    private final PaymentAPI paymentAPI;
    private final Gson gson = new Gson();

    /**
     * PaymentAPI를 주입받는 생성자
     *
     * @param paymentAPI 결제 API 구현체
     */
    public PaymentController(PaymentAPI paymentAPI) {
        if (paymentAPI == null) {
            throw new IllegalArgumentException("PaymentAPI는 null일 수 없습니다");
        }
        this.paymentAPI = paymentAPI;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if ("POST".equals(exchange.getRequestMethod())) {
                handlePostRequest(exchange);
            } else {
                handleUnsupportedMethod(exchange);
            }
        } catch (Exception e) {
            log.error("결제 처리 중 오류 발생: {}", e.getMessage(), e);
            sendErrorResponse(exchange, 500, "서버 내부 오류: " + e.getMessage());
        }
    }

    private void handlePostRequest(HttpExchange exchange) throws IOException {
        // 요청 본문 읽기
        BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8));
        StringBuilder requestBody = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            requestBody.append(line);
        }

        // 요청 파라미터 파싱
        String jsonBody = requestBody.toString();
        JsonObject requestJson = gson.fromJson(jsonBody, JsonObject.class);

        String paymentKey = getStringOrDefault(requestJson, "paymentKey", "");
        String orderId = getStringOrDefault(requestJson, "orderId", "");
        String amount = getStringOrDefault(requestJson, "amount", "0");
        String userId = getStringOrDefault(requestJson, "userId", java.util.UUID.randomUUID().toString());
        String creditAmount = getStringOrDefault(requestJson, "creditAmount", "0");

        log.info("결제 검증 요청: paymentKey={}, orderId={}, amount={}, userId={}, creditAmount={}",
                paymentKey, orderId, amount, userId, creditAmount);

        try {
            // 1. 결제 정보를 확인하고 게임 서버에 전송
            JsonObject result = paymentAPI.sendPaymentToGameServer(requestJson, userId, creditAmount);

            // 2. 응답 생성
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.has("success") && result.get("success").getAsBoolean());
            response.put("paymentKey", paymentKey);
            response.put("orderId", orderId);
            response.put("amount", amount);
            response.put("userId", userId);
            response.put("creditAmount", creditAmount);

            // 메시지 추가
            if (result.has("message")) {
                response.put("message", result.get("message").getAsString());
            } else {
                response.put("message", "결제가 성공적으로 처리되었습니다.");
            }

            // 게임 서버 응답 추가
            response.put("gameServerResponse", result);

            // 성공 응답 전송
            sendJsonResponse(exchange, 200, gson.toJson(response));
        } catch (Exception e) {
            log.error("결제 검증 실패: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "결제 검증 중 오류가 발생했습니다: " + e.getMessage());
            errorResponse.put("paymentKey", paymentKey);
            errorResponse.put("orderId", orderId);

            sendJsonResponse(exchange, 400, gson.toJson(errorResponse));
        }
    }

    /**
     * JsonObject에서 문자열 값을 안전하게 가져오는 유틸리티 메소드
     */
    private String getStringOrDefault(JsonObject json, String key, String defaultValue) {
        if (json.has(key) && !json.get(key).isJsonNull()) {
            return json.get(key).getAsString();
        }
        return defaultValue;
    }

    /**
     * 요청 본문 파싱
     */
    private Map<String, String> parseRequestBody(String body) {
        Map<String, String> result = new HashMap<>();
        try {
            JsonObject jsonObject = gson.fromJson(body, JsonObject.class);

            for (String key : jsonObject.keySet()) {
                if (!jsonObject.get(key).isJsonNull()) {
                    result.put(key, jsonObject.get(key).getAsString());
                }
            }
        } catch (Exception e) {
            log.warn("요청 본문 파싱 실패: {}", e.getMessage());
        }
        return result;
    }

    private void handleUnsupportedMethod(HttpExchange exchange) throws IOException {
        sendErrorResponse(exchange, 405, "지원하지 않는 HTTP 메소드입니다");
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, String responseBody) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        byte[] responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
            os.flush();
        } catch (IOException e) {
            log.debug("응답 전송 중 클라이언트 연결 종료: {}", e.getMessage());
        }
    }

    private void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", message);

        sendJsonResponse(exchange, statusCode, gson.toJson(errorResponse));
    }
}
