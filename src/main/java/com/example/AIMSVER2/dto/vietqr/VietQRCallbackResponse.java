package com.example.AIMSVER2.dto.vietqr;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho callback response gửi về VietQR
 * Format theo tài liệu VietQR transaction-sync API
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VietQRCallbackResponse {
    
    private boolean error; // false = success, true = failure
    
    private String errorReason; // Mã lỗi trả về từ đối tác
    
    private String toastMessage; // Mô tả lỗi trả về từ đối tác
    
    private ResponseObject object; // Object chứa thông tin (null nếu error = true)
    
    /**
     * Inner class cho object trong response
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ResponseObject {
        @com.fasterxml.jackson.annotation.JsonProperty("reftransactionid")
        private String refTransactionId; // ID của giao dịch
    }
}
