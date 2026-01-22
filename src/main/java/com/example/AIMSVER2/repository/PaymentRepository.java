package com.example.AIMSVER2.repository;

import com.example.AIMSVER2.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    Optional<Payment> findByPaymentCode(String paymentCode);
    Optional<Payment> findByTransactionId(String transactionId);
    
    @Query("SELECT p FROM Payment p WHERE p.order.id = :orderId AND p.paymentMethod = :paymentMethod AND p.status = :status")
    List<Payment> findByOrderIdAndPaymentMethodAndStatus(
        @Param("orderId") Integer orderId,
        @Param("paymentMethod") String paymentMethod,
        @Param("status") String status
    );
}
