package com.febrie.payment.handler;

import com.febrie.payment.api.PaymentAPI;
import com.febrie.util.Logging;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PaymentHandler implements HttpHandler {
    private static final Logger log = Logging.getLogger(PaymentHandler.class);

    private final PaymentAPI paymentAPI;
    private final Gson gson = new Gson();
    
    /**
     * PaymentAPI를 주입받는 생성자
     * @param paymentAPI 결제 API 구현체
     */
    public PaymentHandler(PaymentAPI paymentAPI) {
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
            sendErrorResponse(exchange, 500, "서버 내부 오류: " + e.getMessage());
        }
    }

    private void handlePostRequest(@NotNull HttpExchange exchange) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8));
        StringBuilder requestBody = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            requestBody.append(line);
        }

        try {
            // 디버깅용: 전체 요청 본문 로깅
            String rawRequestBody = requestBody.toString();
            Logging.debug(log, "수신된 토스페이먼츠 결제 원본 데이터: {}", rawRequestBody);
            
            // 요청 본문을 JsonObject로 파싱
            JsonObject requestJson = JsonParser.parseString(rawRequestBody).getAsJsonObject();
            
            // 요청 헤더 로깅
            Logging.debug(log, "요청 헤더:");
            for (String headerName : exchange.getRequestHeaders().keySet()) {
                Logging.debug(log, "  {} = {}", headerName, exchange.getRequestHeaders().getFirst(headerName));
            }

            // 필수 파라미터 추출
            String paymentKey = getString(requestJson, "paymentKey");
            String orderId = getString(requestJson, "orderId");
            String amount = getString(requestJson, "amount");
            String userId = getString(requestJson, "userId", UUID.randomUUID().toString()); // 기본값 제공
            String creditAmount = getString(requestJson, "creditAmount", "0"); // 기본값 제공

            // 금액을 정수로 변환
            int amountValue = 0;
            try {
                amountValue = Integer.parseInt(amount);
            } catch (NumberFormatException e) {
                log.warn("금액 파싱 실패: {}, 기본값 0으로 설정", amount);
            }
    
            // 포맷된 JSON으로 로깅 (디버깅용)
            Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
            log.info("토스페이먼츠 결제 데이터(포맷팅됨):\n{}", prettyGson.toJson(requestJson));
    
            log.info("💰 토스페이먼츠 결제 처리: paymentKey={}, orderId={}, amount={}, userId={}, creditAmount={}",
                    paymentKey, orderId, amount, userId, creditAmount);
            
            // 300원 미만 결제인 경우 자동 성공 처리
            if (amountValue < 300) {
                log.info("✅ 300원 미만 결제 자동 성공 처리: {}원", amountValue);
                
                // 자동 성공 응답 생성
                JsonObject autoSuccessResult = new JsonObject();
                autoSuccessResult.addProperty("success", true);
                autoSuccessResult.addProperty("message", "소액 결제 자동 성공 처리되었습니다");
                autoSuccessResult.addProperty("autoProcessed", true);
                autoSuccessResult.addProperty("originalAmount", amountValue);
                autoSuccessResult.addProperty("userId", userId);
                autoSuccessResult.addProperty("creditAmount", creditAmount);
                autoSuccessResult.addProperty("timestamp", System.currentTimeMillis());
                
                // 응답 생성
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("paymentKey", paymentKey);
                response.put("orderId", orderId);
                response.put("amount", amount);
                response.put("userId", userId);
                response.put("creditAmount", creditAmount);
                response.put("message", "소액 결제 자동 성공 처리되었습니다 (300원 미만)");
                response.put("autoProcessed", true);
                response.put("gameServerResponse", autoSuccessResult);
                
                // 성공 응답 전송
                sendJsonResponse(exchange, 200, gson.toJson(response));
                return;
            }

            // 게임 서버에 결제 정보 전송
            JsonObject gameServerResult = paymentAPI.sendPaymentToGameServer(requestJson, userId, creditAmount);

            // 응답 생성
            Map<String, Object> response = new HashMap<>();
            response.put("success", gameServerResult.has("success") && gameServerResult.get("success").getAsBoolean());
            response.put("paymentKey", paymentKey);
            response.put("orderId", orderId);
            response.put("amount", amount);
            response.put("userId", userId);
            response.put("creditAmount", creditAmount);

            // 게임 서버 응답 메시지가 있으면 추가
            if (gameServerResult.has("message")) {
                response.put("message", gameServerResult.get("message").getAsString());
            } else {
                response.put("message", "결제 정보가 게임 서버로 전송되었습니다.");
            }

            // 게임 서버 응답 데이터 추가
            response.put("gameServerResponse", gameServerResult);

            // 성공 응답 전송
            sendJsonResponse(exchange, 200, gson.toJson(response));
        } catch (Exception e) {
            log.error("❌ 결제 정보 처리 중 오류 발생: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "결제 정보 처리 중 오류가 발생했습니다: " + e.getMessage());

            sendJsonResponse(exchange, 400, gson.toJson(errorResponse));
        }
    }

    // JsonObject에서 문자열 값을 안전하게 추출하는 헬퍼 메소드
    private String getString(JsonObject json, String key) {
        return getString(json, key, null);
    }

    private String getString(JsonObject json, String key, String defaultValue) {
        if (json.has(key) && !json.get(key).isJsonNull()) {
            return json.get(key).getAsString();
        }
        if (defaultValue != null) {
            return defaultValue;
        }
        throw new IllegalArgumentException("필수 파라미터가 누락되었습니다: " + key);
    }

    private void handleUnsupportedMethod(HttpExchange exchange) throws IOException {
        sendErrorResponse(exchange, 405, "지원하지 않는 HTTP 메소드입니다");
    }

    private void sendJsonResponse(@NotNull HttpExchange exchange, int statusCode, @NotNull String responseBody) throws IOException {
        try {
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            byte[] responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, responseBytes.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
                os.flush();
            } catch (IOException e) {
                // 클라이언트 연결 종료로 인한 예외는 로깅만 하고 진행
                log.debug("응답 전송 중 클라이언트 연결 종료: {}", e.getMessage());
            }
        } catch (IOException e) {
            // 헤더 전송 실패도 로깅만 하고 진행
            log.warn("응답 헤더 전송 실패: {}", e.getMessage());
            try {
                exchange.close();
            } catch (Exception ignored) {
                // 닫기 실패 무시
            }
        }
    }

    private void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", message);

        sendJsonResponse(exchange, statusCode, gson.toJson(errorResponse));
    }
}
