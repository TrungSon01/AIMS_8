package com.example.AIMSVER2.repository;

import com.example.AIMSVER2.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    Optional<Payment> findByTransactionId(String transactionId);
    // findByPaymentCode sẽ được thêm lại sau khi chạy script ALTER_PAYMENT_TABLE.sql
}
