package com.example.AIMSVER2.config;

import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JpaConfig {
    
    /**
     * Cấu hình để Hibernate không tự động convert camelCase thành snake_case
     * Sử dụng tên cột chính xác như trong @Column annotation
     */
    @Bean
    public PhysicalNamingStrategyStandardImpl physicalNamingStrategy() {
        return new PhysicalNamingStrategyStandardImpl();
    }
}
