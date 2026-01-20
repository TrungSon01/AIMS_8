package com.example.AIMSVER2.factory;

import com.example.AIMSVER2.strategy.PaymentStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PaymentStrategyFactory {
    
    private final List<PaymentStrategy> paymentStrategies;
    private Map<String, PaymentStrategy> strategyMap;
    
    /**
     * Lazy initialization của strategy map
     */
    private void initializeStrategyMap() {
        if (strategyMap == null) {
            strategyMap = paymentStrategies.stream()
                .collect(Collectors.toMap(
                    PaymentStrategy::getPaymentMethod,
                    Function.identity()
                ));
        }
    }
    
    /**
     * Lấy payment strategy theo payment method
     */
    public PaymentStrategy getStrategy(String paymentMethod) {
        initializeStrategyMap();
        PaymentStrategy strategy = strategyMap.get(paymentMethod.toUpperCase());
        if (strategy == null) {
            throw new IllegalArgumentException(
                "Payment method not supported: " + paymentMethod
            );
        }
        return strategy;
    }
}
