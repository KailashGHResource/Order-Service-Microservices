package com.example.order_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundPaymentEvent {
    private Long orderId;
    private Long customerId;
    private BigDecimal amount;
    private String reason;
}