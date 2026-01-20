package com.example.AIMSVER2.strategy.impl;

import com.example.AIMSVER2.dto.PaymentRequest;
import com.example.AIMSVER2.dto.PaymentResponse;
import com.example.AIMSVER2.service.paypal.PayPalService;
import com.example.AIMSVER2.strategy.PaymentStrategy;
import com.paypal.api.payments.Links;
import com.paypal.api.payments.Payment;
import com.paypal.base.rest.PayPalRESTException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PayPalPaymentStrategy implements PaymentStrategy {
    
    private final PayPalService payPalService;
    private final com.example.AIMSVER2.config.PayPalConfig payPalConfig;
    
    @Value("${server.port:8080}")
    private String serverPort;
    
    @Value("${server.servlet.context-path:}")
    private String contextPath;
    
    @Override
    public PaymentResponse createPayment(PaymentRequest request) {
        // Nếu bật chế độ mock, giả lập thanh toán thành công ngay
        if (payPalConfig.isMock()) {
            log.info("MOCK MODE: Simulating successful PayPal payment creation");
            String mockTransactionId = "MOCK-PAY-" + System.currentTimeMillis();
            return PaymentResponse.builder()
                .status("COMPLETED")
                .amount(request.getAmount())
                .description(request.getDescription())
                .paymentMethod("PAYPAL")
                .transactionId(mockTransactionId)
                .createdAt(LocalDateTime.now())
                .message("MOCK MODE: Payment completed successfully (no PayPal redirect needed)")
                .build();
        }
        
        // Chế độ thật - gọi PayPal API
        try {
            String returnUrl = request.getReturnUrl() != null ? request.getReturnUrl() 
                : String.format("http://localhost:%s%s/api/payment/paypal/success", serverPort, contextPath);
            String cancelUrl = request.getCancelUrl() != null ? request.getCancelUrl()
                : String.format("http://localhost:%s%s/api/payment/paypal/cancel", serverPort, contextPath);
            
            Payment payment = payPalService.createPayment(
                request.getAmount(),
                "USD",
                request.getDescription(),
                returnUrl,
                cancelUrl
            );
            
            String approvalUrl = null;
            List<Links> links = payment.getLinks();
            for (Links link : links) {
                if ("approval_url".equals(link.getRel())) {
                    approvalUrl = link.getHref();
                    break;
                }
            }
            
            return PaymentResponse.builder()
                .status("PENDING")
                .amount(request.getAmount())
                .description(request.getDescription())
                .paymentMethod("PAYPAL")
                .approvalUrl(approvalUrl)
                .transactionId(payment.getId())
                .createdAt(LocalDateTime.now())
                .message("Payment created successfully. Please approve the payment.")
                .build();
                
        } catch (PayPalRESTException e) {
            log.error("Error creating PayPal payment: ", e);
            return PaymentResponse.builder()
                .status("FAILED")
                .message("Failed to create PayPal payment: " + e.getMessage())
                .build();
        }
    }
    
    @Override
    public PaymentResponse confirmPayment(String paymentId, String payerId) {
        // Nếu bật chế độ mock, giả lập confirm thành công
        if (payPalConfig.isMock()) {
            log.info("MOCK MODE: Simulating successful payment confirmation");
            return PaymentResponse.builder()
                .status("COMPLETED")
                .transactionId(paymentId)
                .message("MOCK MODE: Payment confirmed successfully")
                .build();
        }
        
        // Chế độ thật - gọi PayPal API
        try {
            Payment payment = payPalService.executePayment(paymentId, payerId);
            
            String status = "PENDING";
            if ("approved".equalsIgnoreCase(payment.getState())) {
                status = "COMPLETED";
            } else if ("failed".equalsIgnoreCase(payment.getState())) {
                status = "FAILED";
            }
            
            return PaymentResponse.builder()
                .status(status)
                .transactionId(payment.getId())
                .message("Payment " + status.toLowerCase())
                .build();
                
        } catch (PayPalRESTException e) {
            log.error("Error confirming PayPal payment: ", e);
            return PaymentResponse.builder()
                .status("FAILED")
                .message("Failed to confirm PayPal payment: " + e.getMessage())
                .build();
        }
    }
    
    @Override
    public PaymentResponse cancelPayment(String paymentId) {
        return PaymentResponse.builder()
            .status("CANCELLED")
            .transactionId(paymentId)
            .message("Payment cancelled by user")
            .build();
    }
    
    @Override
    public String getPaymentMethod() {
        return "PAYPAL";
    }
}
