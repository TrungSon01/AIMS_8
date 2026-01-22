package com.example.AIMSVER2.service.vietqr;

import com.example.AIMSVER2.config.VietQRConfig;
import com.example.AIMSVER2.dto.vietqr.VietQRCallbackRequest;
import com.example.AIMSVER2.entity.Payment;
import com.example.AIMSVER2.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service xử lý callback từ VietQR khi có giao dịch thanh toán
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VietQRCallbackService {
    
    private final PaymentRepository paymentRepository;
    private final VietQRConfig vietQRConfig;
    
    /**
     * Xử lý callback từ VietQR khi có giao dịch thanh toán thành công
     * 
     * @param callbackRequest Request từ VietQR
     * @return true nếu xử lý thành công, false nếu không tìm thấy payment hoặc không khớp
     */
    @Transactional
    public boolean processCallback(VietQRCallbackRequest callbackRequest) {
        // Validate request không null
        if (callbackRequest == null) {
            log.error("Callback request is null");
            return false;
        }
        
        log.info("Received VietQR callback: bankAccount={}, amount={}, content={}, transType={}, bankCode={}, transactionId={}, transactionRefId={}, orderId={}",
            callbackRequest.getBankAccount() != null ? callbackRequest.getBankAccount() : "NULL",
            callbackRequest.getAmount() != null ? callbackRequest.getAmount() : "NULL",
            callbackRequest.getContent() != null ? callbackRequest.getContent() : "NULL",
            callbackRequest.getTransType() != null ? callbackRequest.getTransType() : "NULL",
            callbackRequest.getBankCode() != null ? callbackRequest.getBankCode() : "NULL",
            callbackRequest.getTransactionId() != null ? callbackRequest.getTransactionId() : "NULL",
            callbackRequest.getTransactionRefId() != null ? callbackRequest.getTransactionRefId() : "NULL",
            callbackRequest.getOrderId() != null ? callbackRequest.getOrderId() : "NULL");
        
        // Tìm payment dựa trên các thông tin
        Optional<Payment> paymentOpt = findPaymentByCallback(callbackRequest);
        
        if (paymentOpt.isEmpty()) {
            log.warn("Payment not found for callback: bankAccount={}, amount={}, content={}, bankCode={}",
                callbackRequest.getBankAccount(),
                callbackRequest.getAmount(),
                callbackRequest.getContent(),
                callbackRequest.getBankCode());
            
            // Log tất cả payments VIETQR PENDING để debug
            List<Payment> allPendingPayments = paymentRepository.findAll().stream()
                .filter(p -> "VIETQR".equals(p.getPaymentMethod()))
                .filter(p -> "PENDING".equals(p.getStatus()))
                .toList();
            
            log.warn("Available PENDING VIETQR payments in DB: {}", allPendingPayments.size());
            allPendingPayments.forEach(p -> log.warn("  - Payment ID: {}, Amount: {} USD, Description: {}, TransactionId: {}",
                p.getId(), p.getAmount(), p.getDescription(), p.getTransactionId()));
            
            return false;
        }
        
        Payment payment = paymentOpt.get();
        
        // Đối chiếu thông tin
        if (!validatePayment(callbackRequest, payment)) {
            log.warn("Payment validation failed for paymentId={}, transactionId={}",
                payment.getId(), payment.getTransactionId());
            return false;
        }
        
        // Chỉ xử lý nếu payment đang ở trạng thái PENDING
        if (!"PENDING".equals(payment.getStatus())) {
            log.info("Payment already processed: paymentId={}, status={}", payment.getId(), payment.getStatus());
            return true; // Trả về true vì payment đã được xử lý rồi
        }
        
        // Cập nhật trạng thái payment thành COMPLETED
        payment.setStatus("COMPLETED");
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);
        
        log.info("Payment updated successfully: paymentId={}, paymentCode={}, orderId={}, amount={}, status=COMPLETED",
            payment.getId(),
            payment.getPaymentCode() != null ? payment.getPaymentCode() : "NULL",
            payment.getOrder() != null ? payment.getOrder().getId() : "NULL",
            payment.getAmount());
        
        return true;
    }
    
    /**
     * Tìm payment dựa trên thông tin từ callback
     */
    private Optional<Payment> findPaymentByCallback(VietQRCallbackRequest callbackRequest) {
        // Ưu tiên tìm theo transactionId hoặc transactionRefId
        if (callbackRequest.getTransactionId() != null && !callbackRequest.getTransactionId().isEmpty()) {
            Optional<Payment> byTransactionId = paymentRepository.findByTransactionId(callbackRequest.getTransactionId());
            if (byTransactionId.isPresent()) {
                log.info("Found payment by transactionId: {}", callbackRequest.getTransactionId());
                return byTransactionId;
            }
        }
        
        if (callbackRequest.getTransactionRefId() != null && !callbackRequest.getTransactionRefId().isEmpty()) {
            Optional<Payment> byTransactionRefId = paymentRepository.findByTransactionId(callbackRequest.getTransactionRefId());
            if (byTransactionRefId.isPresent()) {
                log.info("Found payment by transactionRefId: {}", callbackRequest.getTransactionRefId());
                return byTransactionRefId;
            }
        }
        
        // Nếu có orderId, tìm payment theo orderId và các điều kiện khác
        if (callbackRequest.getOrderId() != null && !callbackRequest.getOrderId().isEmpty()) {
            try {
                Integer orderId = Integer.parseInt(callbackRequest.getOrderId());
                List<Payment> payments = paymentRepository.findByOrderIdAndPaymentMethodAndStatus(
                    orderId, "VIETQR", "PENDING");
                
                if (!payments.isEmpty()) {
                    log.info("Found {} payment(s) by orderId: {}", payments.size(), orderId);
                    // Tìm payment khớp nhất với amount và content
                    return payments.stream()
                        .filter(p -> p.getAmount() != null && matchesAmount(callbackRequest.getAmount(), p.getAmount()))
                        .filter(p -> p.getDescription() != null && matchesContent(callbackRequest.getContent(), p.getDescription()))
                        .findFirst();
                }
            } catch (NumberFormatException e) {
                log.warn("Invalid orderId format: {}", callbackRequest.getOrderId());
            }
        }
        
        // Tìm theo amount và content (convert VND sang USD để so sánh)
        // Validate required fields trước
        if (callbackRequest.getAmount() == null) {
            log.warn("Amount is null in callback request");
            return Optional.empty();
        }
        
        if (callbackRequest.getContent() == null || callbackRequest.getContent().isEmpty()) {
            log.warn("Content is null or empty in callback request");
            return Optional.empty();
        }
        
        BigDecimal amountInUSD = convertVndToUsd(callbackRequest.getAmount());
        log.info("Searching payments: amount={} VND ({} USD), content={}, bankAccount={}, bankCode={}",
            callbackRequest.getAmount(), amountInUSD, 
            callbackRequest.getContent() != null ? callbackRequest.getContent() : "NULL",
            callbackRequest.getBankAccount() != null ? callbackRequest.getBankAccount() : "NULL",
            callbackRequest.getBankCode() != null ? callbackRequest.getBankCode() : "NULL");
        
        List<Payment> allPayments = paymentRepository.findAll().stream()
            .filter(p -> p.getPaymentMethod() != null && "VIETQR".equals(p.getPaymentMethod()))
            .filter(p -> p.getStatus() != null && "PENDING".equals(p.getStatus()))
            .peek(p -> log.debug("Checking payment: id={}, amount={}, description={}, status={}",
                p.getId(), p.getAmount(), 
                p.getDescription() != null ? p.getDescription() : "NULL", 
                p.getStatus()))
            .filter(p -> p.getAmount() != null && matchesAmount(callbackRequest.getAmount(), p.getAmount(), amountInUSD))
            .peek(p -> log.debug("Payment {} passed amount check", p.getId()))
            .filter(p -> p.getDescription() != null && matchesContent(callbackRequest.getContent(), p.getDescription()))
            .peek(p -> log.debug("Payment {} passed content check", p.getId()))
            .toList();
        
        log.info("Found {} payment(s) matching amount and content", allPayments.size());
        
        if (!allPayments.isEmpty()) {
            // Nếu có nhiều payment khớp, ưu tiên payment có bankAccount trong description
            // Chỉ filter nếu bankAccount không null
            if (callbackRequest.getBankAccount() != null && !callbackRequest.getBankAccount().isEmpty()) {
                Optional<Payment> byBankAccount = allPayments.stream()
                    .filter(p -> p.getDescription() != null && 
                        p.getDescription().contains(callbackRequest.getBankAccount()))
                    .findFirst();
                
                if (byBankAccount.isPresent()) {
                    log.info("Found payment by bankAccount match: paymentId={}", byBankAccount.get().getId());
                    return byBankAccount;
                }
            }
            
            if (byBankAccount.isPresent()) {
                log.info("Found payment by bankAccount match: paymentId={}", byBankAccount.get().getId());
                return byBankAccount;
            }
            
            // Nếu không có match theo bankAccount, lấy payment đầu tiên
            log.info("Using first matching payment: paymentId={}", allPayments.get(0).getId());
            return Optional.of(allPayments.get(0));
        }
        
        log.warn("No payment found matching all criteria");
        return Optional.empty();
    }
    
    /**
     * Đối chiếu thông tin payment với callback request
     */
    private boolean validatePayment(VietQRCallbackRequest callbackRequest, Payment payment) {
        // Validate null checks
        if (payment == null) {
            log.warn("Payment is null");
            return false;
        }
        
        if (callbackRequest == null) {
            log.warn("Callback request is null");
            return false;
        }
        
        // Kiểm tra payment method phải là VIETQR
        if (payment.getPaymentMethod() == null || !"VIETQR".equals(payment.getPaymentMethod())) {
            log.warn("Payment method mismatch: expected VIETQR, got {}", 
                payment.getPaymentMethod() != null ? payment.getPaymentMethod() : "NULL");
            return false;
        }
        
        // Kiểm tra amount (cho phép sai số nhỏ do làm tròn)
        if (callbackRequest.getAmount() == null || payment.getAmount() == null) {
            log.warn("Amount is null: callback={}, payment={}", 
                callbackRequest.getAmount() != null, payment.getAmount() != null);
            return false;
        }
        
        BigDecimal amountInUSD = convertVndToUsd(callbackRequest.getAmount());
        if (!matchesAmount(callbackRequest.getAmount(), payment.getAmount(), amountInUSD)) {
            log.warn("Amount mismatch: callback={} VND ({} USD), payment={} USD",
                callbackRequest.getAmount(), amountInUSD, payment.getAmount());
            return false;
        }
        
        // Kiểm tra content/description (cho phép không khớp hoàn toàn vì có thể có thêm thông tin)
        // Content từ callback có thể có prefix, nên cần extract phần thực sự
        if (callbackRequest.getContent() == null || payment.getDescription() == null) {
            log.warn("Content or description is null: callbackContent={}, paymentDescription={}",
                callbackRequest.getContent() != null, payment.getDescription() != null);
            // Không return false ngay, chỉ log warning
        } else {
            String extractedContent = extractActualContent(callbackRequest.getContent());
            if (!matchesContent(callbackRequest.getContent(), payment.getDescription())) {
                log.warn("Content mismatch: callback={}, extracted={}, payment={}",
                    callbackRequest.getContent(), extractedContent, payment.getDescription());
                // Không return false ngay, chỉ log warning vì description có thể có thêm thông tin
            } else {
                log.info("Content matched: callback={}, extracted={}, payment={}",
                    callbackRequest.getContent(), extractedContent, payment.getDescription());
            }
        }
        
        // Kiểm tra transType phải là "C" (Credit - nhận tiền)
        if (callbackRequest.getTransType() == null || !"C".equals(callbackRequest.getTransType())) {
            log.warn("Invalid transType: expected C, got {}", 
                callbackRequest.getTransType() != null ? callbackRequest.getTransType() : "NULL");
            return false;
        }
        
        // Kiểm tra bankCode và bankAccount (nếu có trong description)
        if (payment.getDescription() != null && callbackRequest.getBankAccount() != null && !callbackRequest.getBankAccount().isEmpty()) {
            if (!payment.getDescription().contains(callbackRequest.getBankAccount())) {
                log.warn("Bank account mismatch: callback={}, payment description={}",
                    callbackRequest.getBankAccount(), payment.getDescription());
                // Không return false, chỉ log warning
            }
        }
        
        return true;
    }
    
    /**
     * So sánh amount (cho phép sai số nhỏ)
     */
    private boolean matchesAmount(BigDecimal callbackAmountVnd, BigDecimal paymentAmountUsd) {
        if (callbackAmountVnd == null || paymentAmountUsd == null) {
            return false;
        }
        BigDecimal callbackAmountUsd = convertVndToUsd(callbackAmountVnd);
        return matchesAmount(callbackAmountVnd, paymentAmountUsd, callbackAmountUsd);
    }
    
    private boolean matchesAmount(BigDecimal callbackAmountVnd, BigDecimal paymentAmountUsd, BigDecimal callbackAmountUsd) {
        if (callbackAmountVnd == null || paymentAmountUsd == null || callbackAmountUsd == null) {
            return false;
        }
        // So sánh với sai số cho phép 0.01 USD (tương đương ~250 VND)
        BigDecimal difference = paymentAmountUsd.subtract(callbackAmountUsd).abs();
        return difference.compareTo(new BigDecimal("0.01")) <= 0;
    }
    
    /**
     * So sánh content/description (cho phép description có thêm thông tin)
     * Content từ callback có thể có prefix như "VQR26044A327PVJX THANH TOAN HOA DON"
     * Cần extract phần content thực sự để so sánh
     */
    private boolean matchesContent(String callbackContent, String paymentDescription) {
        if (callbackContent == null || paymentDescription == null) {
            return false;
        }
        
        // Extract phần content thực sự (bỏ prefix nếu có)
        // Ví dụ: "VQR26044A327PVJX THANH TOAN HOA DON" -> "THANH TOAN HOA DON"
        String actualContent = extractActualContent(callbackContent);
        
        // So sánh: description có thể chứa actualContent hoặc ngược lại
        boolean matches = paymentDescription.contains(actualContent) || actualContent.contains(paymentDescription);
        
        if (!matches) {
            log.debug("Content comparison: callback={}, extracted={}, payment={}, match={}",
                callbackContent, actualContent, paymentDescription, matches);
        }
        
        return matches;
    }
    
    /**
     * Extract phần content thực sự từ callback content
     * Content có thể có format: "VQR26044A5CCKYZA THANH TOAN HOA DON"
     * Hoặc chỉ có: "THANH TOAN HOA DON"
     * Prefix thường có format: "VQR" + 13 ký tự alphanumeric = 16 ký tự
     */
    private String extractActualContent(String callbackContent) {
        if (callbackContent == null || callbackContent.isEmpty()) {
            return "";
        }
        
        String trimmed = callbackContent.trim();
        
        // Nếu bắt đầu bằng "VQR" và có khoảng trắng, extract phần sau prefix
        // Prefix format: "VQR" + 13 ký tự = 16 ký tự, sau đó là khoảng trắng
        if (trimmed.startsWith("VQR") && trimmed.length() > 16) {
            // Tìm khoảng trắng đầu tiên sau vị trí 16 (sau prefix)
            int spaceIndex = trimmed.indexOf(' ', 16);
            if (spaceIndex > 0 && spaceIndex < trimmed.length() - 1) {
                String extracted = trimmed.substring(spaceIndex + 1).trim();
                log.info("Extracted content: '{}' -> '{}'", callbackContent, extracted);
                return extracted;
            }
        }
        
        // Nếu không có prefix hoặc không tìm thấy khoảng trắng, trả về nguyên content
        log.debug("No prefix found, using original content: '{}'", trimmed);
        return trimmed;
    }
    
    /**
     * Convert VND sang USD
     */
    private BigDecimal convertVndToUsd(BigDecimal amountVnd) {
        if (amountVnd == null) {
            return BigDecimal.ZERO;
        }
        if (vietQRConfig.getUsdToVndRate() == null || vietQRConfig.getUsdToVndRate() == 0) {
            log.warn("USD to VND rate is null or zero, using default 25000");
            return amountVnd.divide(BigDecimal.valueOf(25000.0), 2, RoundingMode.HALF_UP);
        }
        return amountVnd.divide(BigDecimal.valueOf(vietQRConfig.getUsdToVndRate()), 2, RoundingMode.HALF_UP);
    }
}
