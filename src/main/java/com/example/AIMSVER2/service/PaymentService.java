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
        payment.setTransactionId(paymentResponse.getTransactionId());
        payment.setCreatedAt(LocalDateTime.now());
        
        Payment savedPayment = paymentRepository.save(payment);
        
        // Update response with payment ID
        paymentResponse.setPaymentId(savedPayment.getId());
        
        // Generate payment code for response (không lưu vào DB nếu không có cột)
        String paymentCode = generatePaymentCode(request.getPaymentMethod());
        paymentResponse.setPaymentCode(paymentCode);
        
        return paymentResponse;
    }
    
    /**
     * Xác nhận payment (sau khi user approve trên PayPal)
     */
    @Transactional
    public PaymentResponse confirmPayment(String paymentId, String payerId) {
        Payment payment = paymentRepository.findByTransactionId(paymentId)
            .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
        
        // Get payment method from transaction ID or default
        String paymentMethod = "PAYPAL"; // Default, có thể lưu trong transactionId hoặc thêm cột riêng
        PaymentStrategy strategy = paymentStrategyFactory.getStrategy(paymentMethod);
        PaymentResponse response = strategy.confirmPayment(paymentId, payerId);
        
        // Update payment (chỉ các trường có trong database)
        paymentRepository.save(payment);
        
        response.setPaymentId(payment.getId());
        response.setPaymentCode(generatePaymentCode(paymentMethod));
        
        return response;
    }
    
    /**
     * Hủy payment
     */
    @Transactional
    public PaymentResponse cancelPayment(String paymentId) {
        Payment payment = paymentRepository.findByTransactionId(paymentId)
            .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
        
        // Get payment method from transaction ID or default
        String paymentMethod = "PAYPAL"; // Default
        PaymentStrategy strategy = paymentStrategyFactory.getStrategy(paymentMethod);
        PaymentResponse response = strategy.cancelPayment(paymentId);
        
        // Update payment (chỉ các trường có trong database)
        paymentRepository.save(payment);
        
        response.setPaymentId(payment.getId());
        response.setPaymentCode(generatePaymentCode(paymentMethod));
        
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
