package com.febrie.payment.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 토스페이먼츠 결제 응답 모델
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private String paymentKey;        // 토스페이먼츠 결제 키
    private String orderId;           // 주문 ID
    private Long amount;              // 결제 금액
    private String orderName;         // 주문명
    private String status;            // 결제 상태
    private String method;            // 결제 수단
    private String requestedAt;       // 결제 요청 시간
    private String approvedAt;        // 결제 승인 시간
    private String customerKey;       // 고객 키
    private String userId;            // 게임 사용자 ID
    private String creditAmount;      // 크레딧 수량
    private Boolean couponApplied;    // 쿠폰 적용 여부
    private Long originalAmount;      // 할인 전 원래 금액
}
