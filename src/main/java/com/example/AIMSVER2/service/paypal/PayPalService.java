package com.example.AIMSVER2.service.paypal;

import com.example.AIMSVER2.config.PayPalConfig;
import com.paypal.api.payments.*;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayPalService {
    
    private final PayPalConfig payPalConfig;
    
    private APIContext getApiContext() {
        // Log để debug
        log.info("PayPal Config - ClientId: {}, Mode: {}", 
            payPalConfig.getClientId() != null ? payPalConfig.getClientId().substring(0, Math.min(10, payPalConfig.getClientId().length())) + "..." : "NULL",
            payPalConfig.getMode());
        log.info("PayPal Config - ClientSecret: {}", 
            payPalConfig.getClientSecret() != null ? "***CONFIGURED***" : "NULL");
        
        if (payPalConfig.getClientId() == null || payPalConfig.getClientSecret() == null) {
            log.error("PayPal credentials are null! ClientId: {}, ClientSecret: {}", 
                payPalConfig.getClientId(), payPalConfig.getClientSecret());
            throw new IllegalStateException("PayPal credentials are not configured. Please check application.properties. Use format: paypal.client-id and paypal.client-secret");
        }
        
        // PayPal SDK tự động xử lý OAuth2 token với grant_type=client_credentials
        // Không cần gửi grant_type manually
        APIContext apiContext = new APIContext(
            payPalConfig.getClientId(),
            payPalConfig.getClientSecret(),
            payPalConfig.getMode()
        );
        
        return apiContext;
    }
    
    /**
     * Tạo PayPal payment
     */
    public Payment createPayment(BigDecimal amount, String currency, String description, 
                                 String returnUrl, String cancelUrl) throws PayPalRESTException {
        Amount paymentAmount = new Amount();
        paymentAmount.setCurrency(currency);
        paymentAmount.setTotal(String.valueOf(amount.setScale(2, RoundingMode.HALF_UP)));
        
        Transaction transaction = new Transaction();
        transaction.setDescription(description);
        transaction.setAmount(paymentAmount);
        
        List<Transaction> transactions = new ArrayList<>();
        transactions.add(transaction);
        
        Payer payer = new Payer();
        payer.setPaymentMethod("paypal");
        
        Payment payment = new Payment();
        payment.setIntent("sale");
        payment.setPayer(payer);
        payment.setTransactions(transactions);
        
        RedirectUrls redirectUrls = new RedirectUrls();
        redirectUrls.setReturnUrl(returnUrl);
        redirectUrls.setCancelUrl(cancelUrl);
        payment.setRedirectUrls(redirectUrls);
        
        return payment.create(getApiContext());
    }
    
    /**
     * Execute payment sau khi user approve
     */
    public Payment executePayment(String paymentId, String payerId) throws PayPalRESTException {
        Payment payment = new Payment();
        payment.setId(paymentId);
        
        PaymentExecution paymentExecution = new PaymentExecution();
        paymentExecution.setPayerId(payerId);
        
        return payment.execute(getApiContext(), paymentExecution);
    }
    
    /**
     * Lấy payment details
     */
    public Payment getPayment(String paymentId) throws PayPalRESTException {
        return Payment.get(getApiContext(), paymentId);
    }
}
