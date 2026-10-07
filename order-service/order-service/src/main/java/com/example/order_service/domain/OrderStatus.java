package com.example.order_service.domain;

import java.util.Set;

public enum OrderStatus {
    CREATED, CONFIRMED, PAYMENT_PENDING, PAID, PROCESSING, SHIPPED, DELIVERED, CANCELLED, PAYMENT_FAILED, OUT_OF_STOCK;

    static {
        CREATED.allowedTransitions = Set.of(CONFIRMED, CANCELLED, OUT_OF_STOCK);
        CONFIRMED.allowedTransitions = Set.of(PAYMENT_PENDING, CANCELLED);
        PAYMENT_PENDING.allowedTransitions = Set.of(PAID, PAYMENT_FAILED, CANCELLED);
        PAID.allowedTransitions = Set.of(PROCESSING, CANCELLED);
        PROCESSING.allowedTransitions = Set.of(SHIPPED);
        SHIPPED.allowedTransitions = Set.of(DELIVERED);

        DELIVERED.allowedTransitions = Set.of();
        CANCELLED.allowedTransitions = Set.of();
        PAYMENT_FAILED.allowedTransitions = Set.of();
        OUT_OF_STOCK.allowedTransitions = Set.of();
    }

    private Set<OrderStatus> allowedTransitions;

    public boolean canTransitionTo(OrderStatus nextState) {
        return this.allowedTransitions.contains(nextState);
    }
}