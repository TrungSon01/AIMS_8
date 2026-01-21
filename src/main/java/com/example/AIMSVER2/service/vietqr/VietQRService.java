package com.example.AIMSVER2.service.vietqr;

import com.example.AIMSVER2.config.VietQRConfig;
import com.example.AIMSVER2.dto.vietqr.VietQRGenerateRequest;
import com.example.AIMSVER2.dto.vietqr.VietQRGenerateResponse;
import com.example.AIMSVER2.dto.vietqr.VietQRTokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class VietQRService {
    
    private final VietQRConfig vietQRConfig;
    private final RestTemplate restTemplate;
    
    private String cachedToken;
    private long tokenExpiresAt;
    
    /**
     * Lấy access token từ VietQR API
     * Token được cache để tránh gọi API nhiều lần
     */
    public String getAccessToken() {
        // Kiểm tra token cache còn hợp lệ không
        if (cachedToken != null && System.currentTimeMillis() < tokenExpiresAt) {
            log.debug("Using cached VietQR token");
            return cachedToken;
        }
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            // VietQR API yêu cầu Basic Auth với client-id và client-secret
            if (vietQRConfig.getClientId() != null && vietQRConfig.getClientSecret() != null) {
                String auth = Base64.getEncoder().encodeToString(
                    (vietQRConfig.getClientId() + ":" + vietQRConfig.getClientSecret()).getBytes()
                );
                headers.set("Authorization", "Basic " + auth);
            }
            
            // Body với grant_type=client_credentials
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "client_credentials");
            
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            
            // Token API dùng dev.vietqr.org
            String url = vietQRConfig.getTokenApiUrl() + "/token_generate";
            
            ResponseEntity<VietQRTokenResponse> response = restTemplate.postForEntity(
                url,
                request,
                VietQRTokenResponse.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                VietQRTokenResponse tokenResponse = response.getBody();
                cachedToken = tokenResponse.getAccessToken();
                // Cache token với thời gian hết hạn (trừ 10 giây để đảm bảo an toàn)
                tokenExpiresAt = System.currentTimeMillis() + ((tokenResponse.getExpiresIn() - 10) * 1000L);
                log.info("VietQR token obtained successfully, expires in {} seconds", tokenResponse.getExpiresIn());
                return cachedToken;
            } else {
                throw new RuntimeException("Failed to get VietQR token: " + response.getStatusCode());
            }
            
        } catch (RestClientException e) {
            log.error("Error getting VietQR token: ", e);
            throw new RuntimeException("Failed to get VietQR access token: " + e.getMessage(), e);
        }
    }
    
    /**
     * Generate QR code từ VietQR API
     */
    public VietQRGenerateResponse generateQRCode(BigDecimal amount, String content, String orderId) {
        try {
            String accessToken = getAccessToken();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);
            
            VietQRGenerateRequest request = VietQRGenerateRequest.builder()
                .bankCode(vietQRConfig.getBankCode())
                .bankAccount(vietQRConfig.getBankAccount())
                .userBankName(vietQRConfig.getUserBankName())
                .amount(String.valueOf(amount.intValue())) // VietQR API yêu cầu amount là số nguyên (VND)
                .content(content)
                .orderId(orderId)
                .build();
            
            HttpEntity<VietQRGenerateRequest> httpEntity = new HttpEntity<>(request, headers);
            
            // QR Generate API dùng api.vietqr.org
            String url = vietQRConfig.getQrApiUrl() + "/qr/generate-customer";
            
            ResponseEntity<VietQRGenerateResponse> response = restTemplate.postForEntity(
                url,
                httpEntity,
                VietQRGenerateResponse.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("VietQR QR code generated successfully for order: {}", orderId);
                return response.getBody();
            } else {
                throw new RuntimeException("Failed to generate VietQR QR code: " + response.getStatusCode());
            }
            
        } catch (RestClientException e) {
            log.error("Error generating VietQR QR code: ", e);
            throw new RuntimeException("Failed to generate VietQR QR code: " + e.getMessage(), e);
        }
    }
}
