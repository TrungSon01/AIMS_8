package com.example.AIMSVER2.dto.vietqr;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO cho callback request từ VietQR
 */
@Data
public class VietQRCallbackRequest {
    
    @JsonProperty("bankAccount")
    private String bankAccount;
    
    @JsonProperty("content")
    private String content;
    
    @JsonProperty("amount")
    private BigDecimal amount; // Số tiền VND
    
    @JsonProperty("transType")
    private String transType; // "C" = Credit (nhận tiền)
    
    @JsonProperty("bankCode")
    private String bankCode;
    
    // Các field có thể có thêm từ VietQR
    @JsonProperty("transactionId")
    private String transactionId;
    
    @JsonProperty("transactionRefId")
    private String transactionRefId;
    
    @JsonProperty("orderId")
    private String orderId;
    
    @JsonProperty("timestamp")
    private String timestamp;
}
