package com.example.AIMSVER2.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "`Order`")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "customerName", nullable = false)
    private String customerName;
    
    @Column(name = "addressLine")
    private String addressLine;
    
    @Column(name = "shippingFee", columnDefinition = "DECIMAL(10,2) DEFAULT 0")
    private BigDecimal shippingFee;
    
    @Column(name = "createdAt", columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;
    
    @Column(name = "price", columnDefinition = "DECIMAL(10,2)")
    private BigDecimal price;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Payment> payments;
}
