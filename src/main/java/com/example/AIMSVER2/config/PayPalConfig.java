package com.example.AIMSVER2.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Data
@Configuration
@Validated
@ConfigurationProperties(prefix = "paypal")
public class PayPalConfig {
    private String clientId;
    private String clientSecret;
    private String mode;
}
