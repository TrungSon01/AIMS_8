package com.example.AIMSVER2.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Data
@Slf4j
@Configuration
@Validated
@ConfigurationProperties(prefix = "vietqr")
public class VietQRConfig {
    private String apiUrl = "https://dev.vietqr.org/vqr/api";
    private String clientId;
    private String clientSecret;
    private String bankCode;
    private String bankAccount;
    private String userBankName;
    private Double usdToVndRate = 25000.0; // Tỷ giá USD sang VND (mặc định 25000)
    
    @PostConstruct
    public void init() {
        log.info("VietQR Config initialized - API URL: {}, Bank: {}, Account: {}", 
            apiUrl, bankCode, bankAccount != null ? "***CONFIGURED***" : "NULL");
    }
}
