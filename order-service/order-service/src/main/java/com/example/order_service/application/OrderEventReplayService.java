package com.example.order_service.application;

import com.example.order_service.domain.EventStore;
import com.example.order_service.domain.Order;
import com.example.order_service.domain.OrderStatus;
import com.example.order_service.event.OrderCreatedEvent;
import com.example.order_service.repository.EventStoreRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventReplayService {

    private final EventStoreRepository eventStoreRepository;
    private final ObjectMapper objectMapper;

    public Order reconstructOrderState(String orderId) {
        log.info("⏪ Replaying events to reconstruct state for Order ID: {}", orderId);

        // 1. Fetch event stream ordered by version
        List<EventStore> eventHistory = eventStoreRepository.findByAggregateIdOrderByVersionAsc(orderId);

        if (eventHistory.isEmpty()) {
            throw new RuntimeException("No events found in Event Store for Order ID: " + orderId);
        }

        // 2. Create blank state
        Order reconstructedOrder = new Order();

        // 3. Apply events sequentially
        for (EventStore storedEvent : eventHistory) {
            try {
                switch (storedEvent.getEventType()) {
                    case "OrderCreatedEvent":
                        OrderCreatedEvent createdEvent = objectMapper.readValue(storedEvent.getEventData(), OrderCreatedEvent.class);
                        reconstructedOrder.setId(createdEvent.getOrderId());
                        reconstructedOrder.setCustomerId(createdEvent.getCustomerId());
                        reconstructedOrder.setTotalAmount(createdEvent.getTotalAmount());
                        reconstructedOrder.setStatus(OrderStatus.CREATED);
                        break;

                    // --- NEW: Handle Inventory Success (Order Confirmed) ---
                    case "OrderConfirmedEvent":
                        reconstructedOrder.setStatus(OrderStatus.CONFIRMED);
                        // In our Saga flow, confirming inventory immediately puts it into payment pending
                        reconstructedOrder.setStatus(OrderStatus.PAYMENT_PENDING);
                        break;

                    // --- NEW: Handle Payment Success (Order Paid) ---
                    case "OrderPaidEvent":
                        reconstructedOrder.setStatus(OrderStatus.PAID);
                        break;

                    case "OrderCancelledEvent":
                        reconstructedOrder.setStatus(OrderStatus.CANCELLED);
                        break;

                    default:
                        log.warn("Unknown event type during replay: {}", storedEvent.getEventType());
                }
            } catch (Exception e) {
                log.error("Failed to deserialize event payload", e);
            }
        }

        log.info("✅ State successfully reconstructed. Current Status: {}", reconstructedOrder.getStatus());
        return reconstructedOrder;
    }
}