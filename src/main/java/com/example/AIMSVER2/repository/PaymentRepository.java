package com.example.AIMSVER2.repository;

import com.example.AIMSVER2.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    Optional<Payment> findByPaymentCode(String paymentCode);
    Optional<Payment> findByTransactionId(String transactionId);
}
