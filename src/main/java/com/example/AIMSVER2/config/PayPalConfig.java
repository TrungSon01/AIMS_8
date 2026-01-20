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
@ConfigurationProperties(prefix = "paypal")
public class PayPalConfig {
    private String clientId;
    private String clientSecret;
    private String mode;
    private boolean mock = false; // Chế độ mock để test không cần PayPal thật
    
    @PostConstruct
    public void init() {
        log.info("PayPal Config initialized - ClientId: {}, Mode: {}, Secret configured: {}", 
            clientId != null ? clientId.substring(0, Math.min(10, clientId.length())) + "..." : "NULL",
            mode,
            clientSecret != null ? "YES" : "NO");
    }
}
