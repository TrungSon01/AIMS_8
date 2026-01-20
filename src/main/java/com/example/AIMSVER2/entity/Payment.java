package com.example.AIMSVER2.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Payment")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "paymentCode", unique = true, length = 50)
    private String paymentCode;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orderId", nullable = false)
    private Order order;
    
    @Column(name = "amount", nullable = false, columnDefinition = "DECIMAL(10,2)")
    private BigDecimal amount;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "status", length = 20)
    private String status;
    
    @Column(name = "qrCodeUrl", columnDefinition = "TEXT")
    private String qrCodeUrl;
    
    @Column(name = "createdAt", columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;
    
    @Column(name = "expiresAt")
    private LocalDateTime expiresAt;
    
    @Column(name = "paidAt")
    private LocalDateTime paidAt;
    
    @Column(name = "paymentMethod", length = 50)
    private String paymentMethod;
    
    @Column(name = "transactionId", unique = true)
    private String transactionId;
}
