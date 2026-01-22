package com.example.AIMSVER2.strategy.impl;

import com.example.AIMSVER2.config.VietQRConfig;
import com.example.AIMSVER2.dto.PaymentRequest;
import com.example.AIMSVER2.dto.PaymentResponse;
import com.example.AIMSVER2.service.vietqr.VietQRService;
import com.example.AIMSVER2.strategy.PaymentStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class VietQRPaymentStrategy implements PaymentStrategy {
    
    private final VietQRService vietQRService;
    private final VietQRConfig vietQRConfig;
    
    @Override
    public PaymentResponse createPayment(PaymentRequest request) {
        try {
            log.info("Creating VietQR payment for order: {}, amount: {}", 
                request.getOrderId(), request.getAmount());
            
            // Generate QR code từ VietQR API
            // Lưu ý: VietQR API yêu cầu amount là VND (số nguyên)
            // Convert amount từ USD sang VND nếu cần
            BigDecimal amountInVND = request.getAmount().multiply(
                BigDecimal.valueOf(vietQRConfig.getUsdToVndRate())
            );
            
            log.info("Converting amount from USD {} to VND {}", request.getAmount(), amountInVND);
            
            var qrResponse = vietQRService.generateQRCode(
                amountInVND,
                request.getDescription(),
                String.valueOf(request.getOrderId())
            );
            
            // Log để debug
            log.info("VietQR Response - qrLink: {}, qrCode: {}, bankName: {}, bankAccount: {}", 
                qrResponse.getQrLink() != null ? qrResponse.getQrLink().substring(0, Math.min(50, qrResponse.getQrLink().length())) + "..." : "NULL",
                qrResponse.getQrCode() != null ? "PRESENT" : "NULL",
                qrResponse.getBankName(),
                qrResponse.getBankAccount());
            
            // Convert amount từ VND sang BigDecimal (nếu cần)
            // VietQR trả về amount là String (VND), nhưng request có thể là USD
            // Giữ nguyên amount từ request
            
            PaymentResponse.PaymentResponseBuilder builder = PaymentResponse.builder()
                .status("PENDING")
                .amount(request.getAmount())
                .description(request.getDescription())
                .paymentMethod("VIETQR")
                .qrCodeUrl(qrResponse.getQrLink()) // URL để hiển thị QR code page (https://pro.vietqr.vn/qr-generated?token=...)
                .qrCode(qrResponse.getQrCode()) // QR code data string (EMV format, not base64 image)
                .transactionId(qrResponse.getTransactionRefId()) // Transaction reference ID từ VietQR
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(15)); // QR code thường expire sau 15 phút
            
            // Thêm thông tin bank nếu có
            if (qrResponse.getBankName() != null) {
                builder.bankName(qrResponse.getBankName());
            }
            if (qrResponse.getBankAccount() != null) {
                builder.bankAccount(qrResponse.getBankAccount());
            }
            
            builder.message(String.format("QR code generated. Bank: %s, Account: %s. Please scan QR code to pay.", 
                qrResponse.getBankName() != null ? qrResponse.getBankName() : "N/A",
                qrResponse.getBankAccount() != null ? qrResponse.getBankAccount() : "N/A"));
            
            return builder.build();
                
        } catch (Exception e) {
            log.error("Error creating VietQR payment: ", e);
            return PaymentResponse.builder()
                .status("FAILED")
                .message("Failed to create VietQR payment: " + e.getMessage())
                .build();
        }
    }
    
    @Override
    public PaymentResponse confirmPayment(String paymentId, String payerId) {
        // VietQR không có API để confirm payment tự động
        // Payment được confirm khi user scan QR và thanh toán
        // Có thể implement webhook hoặc polling để check payment status
        log.info("VietQR payment confirmation requested for transaction: {}", paymentId);
        
        // Trong thực tế, cần gọi API check payment status từ VietQR
        // Hiện tại return PENDING vì không có API để check
        return PaymentResponse.builder()
            .status("PENDING")
            .transactionId(paymentId)
            .message("VietQR payment confirmation - Please check payment status manually or via webhook")
            .build();
    }
    
    @Override
    public PaymentResponse cancelPayment(String paymentId) {
        log.info("VietQR payment cancellation requested for transaction: {}", paymentId);
        
        // VietQR QR code sẽ tự động expire sau thời gian nhất định
        return PaymentResponse.builder()
            .status("CANCELLED")
            .transactionId(paymentId)
            .message("VietQR payment cancelled - QR code will expire")
            .build();
    }
    
    @Override
    public String getPaymentMethod() {
        return "VIETQR";
    }
}
