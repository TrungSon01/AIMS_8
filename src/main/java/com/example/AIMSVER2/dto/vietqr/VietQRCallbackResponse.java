package com.example.AIMSVER2.dto.vietqr;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho callback response gửi về VietQR
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VietQRCallbackResponse {
    
    private String code;
    private String message;
}
