package com.example.AIMSVER2.strategy.impl;

import com.example.AIMSVER2.dto.PaymentRequest;
import com.example.AIMSVER2.dto.PaymentResponse;
import com.example.AIMSVER2.strategy.PaymentStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@Slf4j
public class VietQRPaymentStrategy implements PaymentStrategy {
    
    @Override
    public PaymentResponse createPayment(PaymentRequest request) {
        // Mock implementation cho VietQR
        // Trong thực tế sẽ tích hợp với VietQR API
        String qrCodeUrl = "https://api.vietqr.io/image/qr-demo.png";
        String paymentCode = "VIETQR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        return PaymentResponse.builder()
            .paymentCode(paymentCode)
            .status("PENDING")
            .amount(request.getAmount())
            .description(request.getDescription())
            .paymentMethod("VIETQR")
            .qrCodeUrl(qrCodeUrl)
            .createdAt(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusMinutes(15))
            .message("QR code generated. Please scan to pay.")
            .build();
    }
    
    @Override
    public PaymentResponse confirmPayment(String paymentId, String payerId) {
        // Mock implementation
        return PaymentResponse.builder()
            .status("COMPLETED")
            .transactionId(paymentId)
            .message("VietQR payment confirmed")
            .build();
    }
    
    @Override
    public PaymentResponse cancelPayment(String paymentId) {
        return PaymentResponse.builder()
            .status("CANCELLED")
            .transactionId(paymentId)
            .message("VietQR payment cancelled")
            .build();
    }
    
    @Override
    public String getPaymentMethod() {
        return "VIETQR";
    }
}
