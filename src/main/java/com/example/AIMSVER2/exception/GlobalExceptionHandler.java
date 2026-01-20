package com.example.AIMSVER2.exception;

import com.example.AIMSVER2.dto.PaymentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<PaymentResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("Illegal argument: ", e);
        PaymentResponse response = PaymentResponse.builder()
            .status("FAILED")
            .message(e.getMessage())
            .build();
        return ResponseEntity.badRequest().body(response);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<PaymentResponse> handleGenericException(Exception e) {
        log.error("Unexpected error: ", e);
        PaymentResponse response = PaymentResponse.builder()
            .status("FAILED")
            .message("Internal server error: " + e.getMessage())
            .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
