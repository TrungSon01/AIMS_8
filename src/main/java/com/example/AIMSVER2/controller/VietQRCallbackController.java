package com.example.AIMSVER2.controller;

import com.example.AIMSVER2.dto.vietqr.VietQRCallbackRequest;
import com.example.AIMSVER2.dto.vietqr.VietQRCallbackResponse;
import com.example.AIMSVER2.service.vietqr.VietQRCallbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller xử lý callback từ VietQR
 */
@RestController
@RequestMapping("/vqr/bank/api")
@RequiredArgsConstructor
@Slf4j
public class VietQRCallbackController {
    
    private final VietQRCallbackService vietQRCallbackService;
    
    /**
     * Endpoint nhận callback từ VietQR khi có giao dịch thanh toán
     * 
     * URL: https://api.aims-group3.click/bank/api/transaction-sync
     * Method: POST
     */
    @PostMapping("/transaction-sync")
    public ResponseEntity<VietQRCallbackResponse> handleTransactionSync(
            @RequestBody VietQRCallbackRequest request) {
        
        // Validate request không null
        if (request == null) {
            log.error("Request body is null");
            VietQRCallbackResponse response = VietQRCallbackResponse.builder()
                .error(true)
                .errorReason("99")
                .toastMessage("Invalid request: Request body is null")
                .object(null)
                .build();
            return ResponseEntity.badRequest().body(response);
        }
        
        log.info("=== VietQR Callback Received ===");
        log.info("Bank Account: {}", request.getBankAccount() != null ? request.getBankAccount() : "NULL");
        log.info("Amount: {} VND", request.getAmount() != null ? request.getAmount() : "NULL");
        log.info("Content: {}", request.getContent() != null ? request.getContent() : "NULL");
        log.info("Transaction Type: {}", request.getTransType() != null ? request.getTransType() : "NULL");
        log.info("Bank Code: {}", request.getBankCode() != null ? request.getBankCode() : "NULL");
        log.info("Transaction ID: {}", request.getTransactionId() != null ? request.getTransactionId() : "NULL");
        log.info("Reference Number: {}", request.getReferenceNumber() != null ? request.getReferenceNumber() : "NULL");
        log.info("Transaction Time: {}", request.getTransactionTime() != null ? request.getTransactionTime() : "NULL");
        log.info("Order ID: {}", request.getOrderId() != null ? request.getOrderId() : "NULL");
        log.info("=================================");
        
        try {
            // Xử lý callback
            boolean success = vietQRCallbackService.processCallback(request);
            
            if (success) {
                log.info("Transaction processed successfully");
                // Lấy transactionId từ request để trả về trong object
                String refTransactionId = request.getReferenceNumber() != null ? 
                    request.getReferenceNumber() : 
                    (request.getTransactionId() != null ? request.getTransactionId() : "N/A");
                
                VietQRCallbackResponse.ResponseObject responseObject = 
                    VietQRCallbackResponse.ResponseObject.builder()
                        .refTransactionId(refTransactionId)
                        .build();
                
                VietQRCallbackResponse response = VietQRCallbackResponse.builder()
                    .error(false)
                    .errorReason("00")
                    .toastMessage("Transaction processed successfully")
                    .object(responseObject)
                    .build();
                return ResponseEntity.ok(response);
            } else {
                log.warn("Transaction processing failed - Payment not found or validation failed");
                VietQRCallbackResponse response = VietQRCallbackResponse.builder()
                    .error(true)
                    .errorReason("01")
                    .toastMessage("Transaction not found or validation failed")
                    .object(null)
                    .build();
                return ResponseEntity.ok(response); // Vẫn trả 200 nhưng error = true
            }
            
        } catch (Exception e) {
            log.error("Error processing VietQR callback: ", e);
            VietQRCallbackResponse response = VietQRCallbackResponse.builder()
                .error(true)
                .errorReason("99")
                .toastMessage("Internal server error: " + e.getMessage())
                .object(null)
                .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
