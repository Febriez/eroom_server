package com.febrie.payment.api;

import com.google.gson.JsonObject;

/**
 * 결제 API 인터페이스
 */
public interface PaymentAPI {
    /**
     * 토스페이먼츠 결제 정보를 게임 서버로 전송
     * 
     * @param paymentInfo 토스페이먼츠 결제 정보
     * @param userId 사용자 ID
     * @param creditAmount 크레딧 수량
     * @return 게임 서버 응답 정보
     */
    JsonObject sendPaymentToGameServer(JsonObject paymentInfo, String userId, String creditAmount);
}
