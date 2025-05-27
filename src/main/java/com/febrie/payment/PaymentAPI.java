package com.febrie.payment;


import java.util.Map;

/**
 * 결제 처리를 위한 API 인터페이스
 */
public interface PaymentAPI {
    /**
     * 결제 정보를 검증합니다.
     *
     * @param paymentId 결제 ID
     * @param amount    결제 금액
     * @return 검증 결과 (true: 성공, false: 실패)
     */
    boolean verifyPayment(String paymentId, double amount);

    /**
     * 결제를 처리합니다.
     *
     * @param userId    사용자 ID
     * @param amount    결제 금액
     * @param productId 상품 ID
     * @return 처리된 결제 ID
     */
    String processPayment(String userId, double amount, String productId);

    /**
     * 결제 성공 후 처리를 수행합니다.
     *
     * @param paymentData 결제 데이터
     * @return 처리 결과
     */
    Map<String, Object> handleSuccessPayment(Map<String, Object> paymentData);
}
