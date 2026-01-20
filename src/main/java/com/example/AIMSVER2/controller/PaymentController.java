package com.example.AIMSVER2.controller;

import com.example.AIMSVER2.dto.PaymentRequest;
import com.example.AIMSVER2.dto.PaymentResponse;
import com.example.AIMSVER2.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class PaymentController {
    
    private final PaymentService paymentService;
    
    /**
     * Tạo payment mới
     */
    @PostMapping("/create")
    public ResponseEntity<PaymentResponse> createPayment(@RequestBody PaymentRequest request) {
        try {
            PaymentResponse response = paymentService.createPayment(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Error creating payment: ", e);
            PaymentResponse errorResponse = PaymentResponse.builder()
                .status("FAILED")
                .message(e.getMessage())
                .build();
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            log.error("Unexpected error creating payment: ", e);
            PaymentResponse errorResponse = PaymentResponse.builder()
                .status("FAILED")
                .message("Internal server error: " + e.getMessage())
                .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * PayPal success callback
     */
    @GetMapping("/paypal/success")
    public ResponseEntity<Map<String, Object>> paypalSuccess(
            @RequestParam("paymentId") String paymentId,
            @RequestParam("PayerID") String payerId) {
        Map<String, Object> response = new HashMap<>();
        try {
            PaymentResponse paymentResponse = paymentService.confirmPayment(paymentId, payerId);
            response.put("success", true);
            response.put("message", "Payment completed successfully!");
            response.put("payment", paymentResponse);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error confirming PayPal payment: ", e);
            response.put("success", false);
            response.put("message", "Payment confirmation failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * PayPal cancel callback
     */
    @GetMapping("/paypal/cancel")
    public ResponseEntity<Map<String, Object>> paypalCancel(
            @RequestParam(value = "token", required = false) String paymentId) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (paymentId != null) {
                PaymentResponse paymentResponse = paymentService.cancelPayment(paymentId);
                response.put("success", true);
                response.put("message", "Payment cancelled");
                response.put("payment", paymentResponse);
            } else {
                response.put("success", true);
                response.put("message", "Payment cancelled by user");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error cancelling PayPal payment: ", e);
            response.put("success", false);
            response.put("message", "Error cancelling payment: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Confirm payment manually (for testing or webhook)
     */
    @PostMapping("/confirm")
    public ResponseEntity<PaymentResponse> confirmPayment(
            @RequestParam("paymentId") String paymentId,
            @RequestParam("payerId") String payerId) {
        try {
            PaymentResponse response = paymentService.confirmPayment(paymentId, payerId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Error confirming payment: ", e);
            PaymentResponse errorResponse = PaymentResponse.builder()
                .status("FAILED")
                .message(e.getMessage())
                .build();
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            log.error("Unexpected error confirming payment: ", e);
            PaymentResponse errorResponse = PaymentResponse.builder()
                .status("FAILED")
                .message("Internal server error: " + e.getMessage())
                .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
