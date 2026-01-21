package com.example.AIMSVER2.dto.vietqr;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class VietQRGenerateResponse {
    @JsonProperty("bankCode")
    private String bankCode;
    
    @JsonProperty("bankName")
    private String bankName;
    
    @JsonProperty("bankAccount")
    private String bankAccount;
    
    @JsonProperty("userBankName")
    private String userBankName;
    
    @JsonProperty("amount")
    private String amount;
    
    @JsonProperty("content")
    private String content;
    
    @JsonProperty("qrCode")
    private String qrCode;
    
    @JsonProperty("imgId")
    private String imgId;
    
    @JsonProperty("existing")
    private Integer existing;
    
    @JsonProperty("transactionId")
    private String transactionId;
    
    @JsonProperty("transactionRefId")
    private String transactionRefId;
    
    @JsonProperty("qrLink")
    private String qrLink;
    
    @JsonProperty("terminalCode")
    private String terminalCode;
    
    @JsonProperty("subTerminalCode")
    private String subTerminalCode;
    
    @JsonProperty("serviceCode")
    private String serviceCode;
    
    @JsonProperty("orderId")
    private String orderId;
    
    @JsonProperty("additionalData")
    private List<Object> additionalData;
    
    @JsonProperty("vaAccount")
    private String vaAccount;
}
