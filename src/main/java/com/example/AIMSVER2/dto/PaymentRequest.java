package com.example.AIMSVER2.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
    private Integer orderId;
    private BigDecimal amount;
    private String description;
    private String paymentMethod;
    private String returnUrl;
    private String cancelUrl;
}
