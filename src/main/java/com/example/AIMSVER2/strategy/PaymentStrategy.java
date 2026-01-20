package com.example.AIMSVER2.strategy;

import com.example.AIMSVER2.dto.PaymentRequest;
import com.example.AIMSVER2.dto.PaymentResponse;

public interface PaymentStrategy {
    /**
     * Tạo payment request và trả về payment response
     */
    PaymentResponse createPayment(PaymentRequest request);
    
    /**
     * Xác nhận payment sau khi người dùng thanh toán
     */
    PaymentResponse confirmPayment(String paymentId, String payerId);
    
    /**
     * Hủy payment
     */
    PaymentResponse cancelPayment(String paymentId);
    
    /**
     * Lấy loại payment method mà strategy này hỗ trợ
     */
    String getPaymentMethod();
}
