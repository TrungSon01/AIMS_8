package com.example.AIMSVER2.service;

import com.example.AIMSVER2.dto.PaymentRequest;
import com.example.AIMSVER2.dto.PaymentResponse;
import com.example.AIMSVER2.entity.Order;
import com.example.AIMSVER2.entity.Payment;
import com.example.AIMSVER2.factory.PaymentStrategyFactory;
import com.example.AIMSVER2.repository.OrderRepository;
import com.example.AIMSVER2.repository.PaymentRepository;
import com.example.AIMSVER2.strategy.PaymentStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentStrategyFactory paymentStrategyFactory;
    
    /**
     * Tạo payment mới
     */
    @Transactional
    public PaymentResponse createPayment(PaymentRequest request) {
        // Validate order exists
        Order order = orderRepository.findById(request.getOrderId())
            .orElseThrow(() -> new IllegalArgumentException("Order not found: " + request.getOrderId()));
        
        // Get payment strategy
        PaymentStrategy strategy = paymentStrategyFactory.getStrategy(request.getPaymentMethod());
        
        // Create payment via strategy
        PaymentResponse paymentResponse = strategy.createPayment(request);
        
        // Save payment to database
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(request.getAmount());
        payment.setDescription(request.getDescription());
        payment.setStatus(paymentResponse.getStatus());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setTransactionId(paymentResponse.getTransactionId());
        payment.setQrCodeUrl(paymentResponse.getQrCodeUrl());
        payment.setCreatedAt(LocalDateTime.now());
        payment.setExpiresAt(paymentResponse.getExpiresAt());
        
        // Generate payment code
        String paymentCode = generatePaymentCode(request.getPaymentMethod());
        payment.setPaymentCode(paymentCode);
        
        Payment savedPayment = paymentRepository.save(payment);
        
        // Update response with payment ID and code
        paymentResponse.setPaymentId(savedPayment.getId());
        paymentResponse.setPaymentCode(savedPayment.getPaymentCode());
        
        return paymentResponse;
    }
    
    /**
     * Xác nhận payment (sau khi user approve trên PayPal)
     */
    @Transactional
    public PaymentResponse confirmPayment(String paymentId, String payerId) {
        Payment payment = paymentRepository.findByTransactionId(paymentId)
            .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
        
        PaymentStrategy strategy = paymentStrategyFactory.getStrategy(payment.getPaymentMethod());
        PaymentResponse response = strategy.confirmPayment(paymentId, payerId);
        
        // Update payment status
        payment.setStatus(response.getStatus());
        if ("COMPLETED".equals(response.getStatus())) {
            payment.setPaidAt(LocalDateTime.now());
        }
        paymentRepository.save(payment);
        
        response.setPaymentId(payment.getId());
        response.setPaymentCode(payment.getPaymentCode());
        
        return response;
    }
    
    /**
     * Hủy payment
     */
    @Transactional
    public PaymentResponse cancelPayment(String paymentId) {
        Payment payment = paymentRepository.findByTransactionId(paymentId)
            .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
        
        PaymentStrategy strategy = paymentStrategyFactory.getStrategy(payment.getPaymentMethod());
        PaymentResponse response = strategy.cancelPayment(paymentId);
        
        // Update payment status
        payment.setStatus("CANCELLED");
        paymentRepository.save(payment);
        
        response.setPaymentId(payment.getId());
        response.setPaymentCode(payment.getPaymentCode());
        
        return response;
    }
    
    /**
     * Generate unique payment code
     */
    private String generatePaymentCode(String paymentMethod) {
        String prefix = paymentMethod.equals("PAYPAL") ? "PAYPAL" : "VIETQR";
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
