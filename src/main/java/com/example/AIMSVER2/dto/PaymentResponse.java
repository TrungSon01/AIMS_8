package com.example.AIMSVER2.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {
    private Integer paymentId;
    private String paymentCode;
    private String status;
    private BigDecimal amount;
    private String description;
    private String paymentMethod;
    private String approvalUrl; // URL để redirect đến PayPal
    private String transactionId;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private String qrCodeUrl; // Cho VietQR - URL để hiển thị QR code
    private String qrCode; // Cho VietQR - QR code string (raw data)
    private String bankName; // Cho VietQR - Tên ngân hàng
    private String bankAccount; // Cho VietQR - Số tài khoản
    private String message;
}
