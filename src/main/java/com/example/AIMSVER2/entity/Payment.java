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
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orderId", nullable = false)
    private Order order;
    
    @Column(nullable = false, columnDefinition = "DECIMAL(10,2)")
    private BigDecimal amount;
    
    @Column(unique = true)
    private String transactionId;
    
    @Column(name = "createdAt", columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;
}
