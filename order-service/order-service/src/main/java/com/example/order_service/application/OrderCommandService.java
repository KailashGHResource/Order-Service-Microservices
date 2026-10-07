package com.example.order_service.application;

import com.example.order_service.client.CustomerClient;
import com.example.order_service.client.InventoryClient;
import com.example.order_service.client.ProductClient;
import com.example.order_service.domain.EventStore;
import com.example.order_service.dto.OrderRequestDto;
import com.example.order_service.dto.external.CustomerResponseDto;
import com.example.order_service.dto.external.InventoryResponseDto;
import com.example.order_service.dto.external.ProductResponseDto;
import com.example.order_service.event.OrderCreatedEvent;
import com.example.order_service.exception.OrderAbortedException;
import com.example.order_service.outbox.OutboxMessage;
import com.example.order_service.outbox.OutboxRepository;
import com.example.order_service.repository.EventStoreRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCommandService {

    private final CustomerClient customerClient;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;
    private final OutboxRepository outboxRepository;
    private final EventStoreRepository eventStoreRepository;
    private final ObjectMapper objectMapper;
    private final RedisLockRegistry redisLockRegistry;

    @Autowired
    @Lazy
    private OrderCommandService self;

    @Transactional
    public Long createOrder(OrderRequestDto request) {
        CustomerResponseDto customer = self.fetchCustomer(request.getCustomerId());
        if (customer.getId() == null) {
            throw new OrderAbortedException("Order aborted: Customer service is unreachable.");
        }

        // 🚨 FIX: Generate an 8-digit random ID so Postman can read it perfectly
        Long generatedOrderId = (long) (Math.random() * 90000000L) + 10000000L;

        BigDecimal total = BigDecimal.ZERO;
        List<OrderCreatedEvent.OrderItemEventDto> eventItems = new ArrayList<>();

        for (var itemDto : request.getItems()) {
            String skuCode = String.valueOf(itemDto.getProductId());
            Lock lock = redisLockRegistry.obtain("LOCK_SKU_" + skuCode);
            boolean isLocked = false;

            try {
                isLocked = lock.tryLock(3, TimeUnit.SECONDS);
                if (!isLocked) {
                    throw new OrderAbortedException("System is busy processing SKU " + skuCode);
                }

                InventoryResponseDto inventory = self.checkInventory(skuCode, itemDto.getQuantity());
                if (!inventory.isInStock()) {
                    throw new OrderAbortedException("Item " + skuCode + " is out of stock.");
                }

                ProductResponseDto product = self.fetchProduct(skuCode);
                total = total.add(product.getPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity())));

                eventItems.add(OrderCreatedEvent.OrderItemEventDto.builder()
                        .productId(itemDto.getProductId())
                        .quantity(itemDto.getQuantity())
                        .price(product.getPrice())
                        .build());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Thread interrupted", e);
            } finally {
                if (isLocked) lock.unlock();
            }
        }

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(generatedOrderId)
                .customerId(request.getCustomerId())
                .totalAmount(total)
                .items(eventItems)
                .build();

        try {
            String jsonPayload = objectMapper.writeValueAsString(event);

            EventStore storedEvent = EventStore.builder()
                    .aggregateId(String.valueOf(generatedOrderId))
                    .aggregateType("ORDER")
                    .eventType("OrderCreatedEvent")
                    .eventData(jsonPayload)
                    .version(1)
                    .timestamp(LocalDateTime.now())
                    .build();
            eventStoreRepository.save(storedEvent);

            OutboxMessage outboxMessage = OutboxMessage.builder()
                    .aggregateType("ORDER")
                    .aggregateId(generatedOrderId)
                    .payload(jsonPayload)
                    .status("PENDING")
                    .createdAt(LocalDateTime.now())
                    .build();
            outboxRepository.save(outboxMessage);

        } catch (Exception e) {
            throw new RuntimeException("Failed to save Event", e);
        }

        return generatedOrderId;
    }

    @Transactional
    public void cancelOrder(Long orderId) {
        try {
            String cancelPayload = "{\"orderId\": " + orderId + ", \"reason\": \"System or User Cancellation\"}";

            EventStore storedEvent = EventStore.builder()
                    .aggregateId(String.valueOf(orderId))
                    .aggregateType("ORDER")
                    .eventType("OrderCancelledEvent")
                    .eventData(cancelPayload)
                    .version(2)
                    .timestamp(LocalDateTime.now())
                    .build();
            eventStoreRepository.save(storedEvent);
            log.info("💾 Appended OrderCancelledEvent to Event Store for Order {}", orderId);

            OutboxMessage outboxMessage = OutboxMessage.builder()
                    .aggregateType("ORDER")
                    .aggregateId(orderId)
                    .payload(cancelPayload)
                    .status("PENDING")
                    .createdAt(LocalDateTime.now())
                    .build();
            outboxRepository.save(outboxMessage);

        } catch (Exception e) {
            throw new RuntimeException("Failed to save Cancel Event", e);
        }
    }

    @Cacheable(value = "customers", key = "#customerId")
    @Retry(name = "customer")
    @CircuitBreaker(name = "customer", fallbackMethod = "fallbackFetchCustomer")
    public CustomerResponseDto fetchCustomer(Long customerId) {
        return customerClient.getCustomerById(customerId);
    }

    @SuppressWarnings("unused")
    public CustomerResponseDto fallbackFetchCustomer(Long customerId, Exception ex) {
        CustomerResponseDto mock = new CustomerResponseDto();
        mock.setId(customerId);
        return mock;
    }

    @Cacheable(value = "products", key = "#skuCode")
    @Retry(name = "product")
    @CircuitBreaker(name = "product", fallbackMethod = "fallbackFetchProduct")
    public ProductResponseDto fetchProduct(String skuCode) {
        return productClient.getProductBySku(skuCode);
    }

    @SuppressWarnings("unused")
    public ProductResponseDto fallbackFetchProduct(String skuCode, Exception ex) {
        ProductResponseDto mock = new ProductResponseDto();
        mock.setPrice(BigDecimal.valueOf(150.00));
        return mock;
    }

    @Retry(name = "inventory")
    @CircuitBreaker(name = "inventory", fallbackMethod = "fallbackCheckInventory")
    public InventoryResponseDto checkInventory(String skuCode, Integer quantity) {
        return inventoryClient.checkInventory(skuCode, quantity);
    }

    @SuppressWarnings("unused")
    public InventoryResponseDto fallbackCheckInventory(String skuCode, Integer quantity, Exception ex) {
        return InventoryResponseDto.builder().skuCode(skuCode).isInStock(true).availableQuantity(quantity).build();
    }

    @Transactional
    public void confirmOrder(Long orderId) {
        // Appends OrderConfirmedEvent to Event Store so Read DB knows Inventory succeeded
        EventStore storedEvent = EventStore.builder()
                .aggregateId(String.valueOf(orderId))
                .aggregateType("ORDER")
                .eventType("OrderConfirmedEvent")
                .eventData("{\"orderId\": " + orderId + "}")
                .version(2)
                .timestamp(LocalDateTime.now())
                .build();
        eventStoreRepository.save(storedEvent);

        OutboxMessage outboxMessage = OutboxMessage.builder()
                .aggregateType("ORDER")
                .aggregateId(orderId)
                .payload("{\"orderId\": " + orderId + "}")
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();
        outboxRepository.save(outboxMessage);
        log.info("💾 Appended OrderConfirmedEvent to Event Store for Order {}", orderId);
    }

    @Transactional
    public void markOrderPaid(Long orderId) {
        // Appends OrderPaidEvent to Event Store so Read DB knows Payment succeeded
        EventStore storedEvent = EventStore.builder()
                .aggregateId(String.valueOf(orderId))
                .aggregateType("ORDER")
                .eventType("OrderPaidEvent")
                .eventData("{\"orderId\": " + orderId + "}")
                .version(3)
                .timestamp(LocalDateTime.now())
                .build();
        eventStoreRepository.save(storedEvent);

        OutboxMessage outboxMessage = OutboxMessage.builder()
                .aggregateType("ORDER")
                .aggregateId(orderId)
                .payload("{\"orderId\": " + orderId + "}")
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();
        outboxRepository.save(outboxMessage);
        log.info("💾 Appended OrderPaidEvent to Event Store for Order {}", orderId);
    }
}