package com.example.AIMSVER2.dto.vietqr;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DTO cho callback request từ VietQR
 * Format theo tài liệu VietQR transaction-sync API
 */
@Data
public class VietQRCallbackRequest {
    
    @JsonProperty("bankaccount")
    private String bankAccount; // Tài khoản ngân hàng tạo mã thanh toán
    
    @JsonProperty("amount")
    private Long amount; // Số tiền giao dịch (VND) - Long type
    
    @JsonProperty("transType")
    private String transType; // Phân loại giao dịch: "D" (Debit) hoặc "C" (Credit)
    
    @JsonProperty("content")
    private String content; // Nội dung chuyển tiền
    
    @JsonProperty("transactionid")
    private String transactionId; // ID của giao dịch
    
    @JsonProperty("transactiontime")
    private Long transactionTime; // Thời gian giao dịch (timestamp milliseconds)
    
    @JsonProperty("referencenumber")
    private String referenceNumber; // Mã giao dịch
    
    @JsonProperty("orderId")
    private String orderId; // Mã đơn hàng
    
    // Các trường bổ sung có thể có (không bắt buộc)
    @JsonProperty("bankCode")
    private String bankCode;
}
