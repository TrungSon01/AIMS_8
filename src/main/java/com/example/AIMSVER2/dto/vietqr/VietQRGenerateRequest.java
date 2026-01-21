package com.example.AIMSVER2.dto.vietqr;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VietQRGenerateRequest {
    @JsonProperty("bankCode")
    private String bankCode;
    
    @JsonProperty("bankAccount")
    private String bankAccount;
    
    @JsonProperty("userBankName")
    private String userBankName;
    
    @JsonProperty("amount")
    private String amount;
    
    @JsonProperty("content")
    private String content;
    
    @JsonProperty("orderId")
    private String orderId;
}
