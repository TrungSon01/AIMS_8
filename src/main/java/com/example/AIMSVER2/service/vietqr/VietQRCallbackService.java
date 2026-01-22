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
        log.info("Received VietQR callback: bankAccount={}, amount={}, content={}, transType={}, bankCode={}, transactionId={}, transactionRefId={}, orderId={}",
            callbackRequest.getBankAccount(),
            callbackRequest.getAmount(),
            callbackRequest.getContent(),
            callbackRequest.getTransType(),
            callbackRequest.getBankCode(),
            callbackRequest.getTransactionId(),
            callbackRequest.getTransactionRefId(),
            callbackRequest.getOrderId());
        
        // Tìm payment dựa trên các thông tin
        Optional<Payment> paymentOpt = findPaymentByCallback(callbackRequest);
        
        if (paymentOpt.isEmpty()) {
            log.warn("Payment not found for callback: bankAccount={}, amount={}, content={}",
                callbackRequest.getBankAccount(),
                callbackRequest.getAmount(),
                callbackRequest.getContent());
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
            payment.getPaymentCode(),
            payment.getOrder().getId(),
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
                        .filter(p -> matchesAmount(callbackRequest.getAmount(), p.getAmount()))
                        .filter(p -> matchesContent(callbackRequest.getContent(), p.getDescription()))
                        .findFirst();
                }
            } catch (NumberFormatException e) {
                log.warn("Invalid orderId format: {}", callbackRequest.getOrderId());
            }
        }
        
        // Tìm theo amount và content (convert VND sang USD để so sánh)
        BigDecimal amountInUSD = convertVndToUsd(callbackRequest.getAmount());
        List<Payment> allPayments = paymentRepository.findAll().stream()
            .filter(p -> "VIETQR".equals(p.getPaymentMethod()))
            .filter(p -> "PENDING".equals(p.getStatus()))
            .filter(p -> matchesAmount(callbackRequest.getAmount(), p.getAmount(), amountInUSD))
            .filter(p -> matchesContent(callbackRequest.getContent(), p.getDescription()))
            .toList();
        
        if (!allPayments.isEmpty()) {
            log.info("Found {} payment(s) by amount and content", allPayments.size());
            // Nếu có nhiều payment khớp, ưu tiên payment có bankAccount trong description
            return allPayments.stream()
                .filter(p -> p.getDescription() != null && 
                    p.getDescription().contains(callbackRequest.getBankAccount()))
                .findFirst()
                .or(() -> allPayments.stream().findFirst());
        }
        
        return Optional.empty();
    }
    
    /**
     * Đối chiếu thông tin payment với callback request
     */
    private boolean validatePayment(VietQRCallbackRequest callbackRequest, Payment payment) {
        // Kiểm tra payment method phải là VIETQR
        if (!"VIETQR".equals(payment.getPaymentMethod())) {
            log.warn("Payment method mismatch: expected VIETQR, got {}", payment.getPaymentMethod());
            return false;
        }
        
        // Kiểm tra amount (cho phép sai số nhỏ do làm tròn)
        BigDecimal amountInUSD = convertVndToUsd(callbackRequest.getAmount());
        if (!matchesAmount(callbackRequest.getAmount(), payment.getAmount(), amountInUSD)) {
            log.warn("Amount mismatch: callback={} VND ({} USD), payment={} USD",
                callbackRequest.getAmount(), amountInUSD, payment.getAmount());
            return false;
        }
        
        // Kiểm tra content/description (cho phép không khớp hoàn toàn vì có thể có thêm thông tin)
        if (!matchesContent(callbackRequest.getContent(), payment.getDescription())) {
            log.warn("Content mismatch: callback={}, payment={}",
                callbackRequest.getContent(), payment.getDescription());
            // Không return false ngay, chỉ log warning vì description có thể có thêm thông tin
        }
        
        // Kiểm tra transType phải là "C" (Credit - nhận tiền)
        if (!"C".equals(callbackRequest.getTransType())) {
            log.warn("Invalid transType: expected C, got {}", callbackRequest.getTransType());
            return false;
        }
        
        // Kiểm tra bankCode và bankAccount (nếu có trong description)
        if (payment.getDescription() != null && callbackRequest.getBankAccount() != null) {
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
        BigDecimal callbackAmountUsd = convertVndToUsd(callbackAmountVnd);
        return matchesAmount(callbackAmountVnd, paymentAmountUsd, callbackAmountUsd);
    }
    
    private boolean matchesAmount(BigDecimal callbackAmountVnd, BigDecimal paymentAmountUsd, BigDecimal callbackAmountUsd) {
        // So sánh với sai số cho phép 0.01 USD (tương đương ~250 VND)
        BigDecimal difference = paymentAmountUsd.subtract(callbackAmountUsd).abs();
        return difference.compareTo(new BigDecimal("0.01")) <= 0;
    }
    
    /**
     * So sánh content/description (cho phép description có thêm thông tin)
     */
    private boolean matchesContent(String callbackContent, String paymentDescription) {
        if (callbackContent == null || paymentDescription == null) {
            return false;
        }
        // Description có thể chứa callbackContent + thêm thông tin khác
        return paymentDescription.contains(callbackContent) || callbackContent.contains(paymentDescription);
    }
    
    /**
     * Convert VND sang USD
     */
    private BigDecimal convertVndToUsd(BigDecimal amountVnd) {
        if (amountVnd == null) {
            return BigDecimal.ZERO;
        }
        return amountVnd.divide(BigDecimal.valueOf(vietQRConfig.getUsdToVndRate()), 2, RoundingMode.HALF_UP);
    }
}
