package com.febrie.payment;

import com.febrie.util.Logging;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * 결제 처리 요청을 처리하는 핸들러
 */
@Slf4j
public class PaymentHandler implements HttpHandler {

    private final PaymentAPI paymentAPI;

    public PaymentHandler(PaymentAPI paymentAPI) {
        this.paymentAPI = paymentAPI;
    }

    @Override
    public void handle(@NotNull HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        try {
            if ("POST".equals(method)) {
                    handlePaymentRequest(exchange);
            } else {
                handleInvalidMethodRequest(exchange);
            }
        } catch (Exception e) {
            log.error("결제 처리 요청 중 오류 발생: {}", e.getMessage(), e);
            sendErrorResponse(exchange, 500, "서버 오류: " + e.getMessage());
        } finally {
            exchange.close();
        }
    }

    /**
     * 결제 처리 요청을 처리합니다.
     */
    private void handlePaymentRequest(HttpExchange exchange) throws IOException {
        Logging.info(log, "결제 처리 요청 수신");

        // 요청 바디 읽기
        String requestBody = readRequestBody(exchange);
        Map<String, Object> requestData = parseJson(requestBody);

        // 필수 파라미터 확인
        if (!requestData.containsKey("uid") || !requestData.containsKey("amount") ||
                !requestData.containsKey("creditAmount")) {
            sendErrorResponse(exchange, 400, "필수 파라미터가 누락되었습니다. (uid, amount, creditAmount)");
            return;
        }

        // 파라미터 추출
        String userId = requestData.get("uid").toString();
        double amount = Double.parseDouble(requestData.get("amount").toString());
        String productId = requestData.getOrDefault("productId", "credit").toString();

        try {
            // 자동 결제 처리 (300원 미만)
            if (amount < 300) {
                Logging.info(log, "소액 결제 자동 처리: userId={}, amount={}", userId, amount);
                Map<String, Object> result = paymentAPI.handleSuccessPayment(requestData);
                sendJsonResponse(exchange, 200, result);
                return;
            }

            // 일반 결제 처리
            String paymentId = paymentAPI.processPayment(userId, amount, productId);

            // 응답 생성
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("paymentId", paymentId);
            responseData.put("message", "결제가 성공적으로 처리되었습니다.");

            Logging.info(log, "결제 처리 성공: userId={}, amount={}, paymentId={}", userId, amount, paymentId);

            // 응답 전송
            sendJsonResponse(exchange, 200, responseData);
        } catch (Exception e) {
            log.error("결제 처리 중 오류 발생: {}", e.getMessage(), e);
            sendErrorResponse(exchange, 500, "결제 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 지원하지 않는 HTTP 메소드 요청을 처리합니다.
     */
    private void handleInvalidMethodRequest(HttpExchange exchange) throws IOException {
        sendErrorResponse(exchange, 405, "지원하지 않는 HTTP 메소드입니다.");
    }

    /**
     * 요청 바디를 문자열로 읽습니다.
     */
    private String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream inputStream = exchange.getRequestBody();
             Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8)) {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        }
    }

    /**
     * JSON 문자열을 Map으로 파싱합니다.
     */
    private Map<String, Object> parseJson(String jsonString) {
        Map<String, Object> result = new HashMap<>();

        if (jsonString == null || jsonString.isEmpty()) {
            return result;
        }

        // 간단한 JSON 파싱 (실제 구현에서는 Jackson이나 Gson 등의 라이브러리 사용 권장)
        jsonString = jsonString.trim();
        if (jsonString.startsWith("{") && jsonString.endsWith("}")) {
            jsonString = jsonString.substring(1, jsonString.length() - 1);

            for (String pair : jsonString.split(",")) {
                String[] keyValue = pair.split(":", 2);
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim().replace("\"", "");
                    String value = keyValue[1].trim().replace("\"", "");

                    // 숫자 변환 시도
                    try {
                        if (value.contains(".")) {
                            result.put(key, Double.parseDouble(value));
                        } else {
                            result.put(key, Integer.parseInt(value));
                        }
                    } catch (NumberFormatException e) {
                        // 숫자가 아니면 문자열로 저장
                        result.put(key, value);
                    }
                }
            }
        }

        return result;
    }

    /**
     * JSON 응답을 전송합니다.
     */
    private void sendJsonResponse(HttpExchange exchange, int statusCode, Map<String, Object> data) throws IOException {
        String jsonResponse = convertToJson(data);

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, jsonResponse.getBytes(StandardCharsets.UTF_8).length);

        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(jsonResponse.getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * 에러 응답을 전송합니다.
     */
    private void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("success", false);
        errorData.put("message", message);

        sendJsonResponse(exchange, statusCode, errorData);
    }

    /**
     * Map을 JSON 문자열로 변환합니다.
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
