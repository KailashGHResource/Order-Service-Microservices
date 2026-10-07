package com.example.order_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class SagaEvents {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryReservedEvent {
        private Long orderId;
        private String status; // "SUCCESS" or "FAILED"
        private String reason;
        @Builder.Default
        private Integer version = 1; // Event Versioning
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentProcessedEvent {
        private Long orderId;
        private String status; // "SUCCESS" or "FAILED"
        private String reason;
        @Builder.Default
        private Integer version = 1; // Event Versioning
    }
}