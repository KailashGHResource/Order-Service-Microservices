package com.example.order_service.domain;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "event_store")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventStore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String aggregateId; // The Order ID

    @Column(nullable = false)
    private String aggregateType; // e.g., "ORDER"

    @Column(nullable = false)
    private String eventType; // e.g., "OrderCreatedEvent", "OrderCancelledEvent"

    @Column(nullable = false, columnDefinition = "TEXT")
    private String eventData; // The JSON representation of the event

    @Column(nullable = false)
    private Integer version; // For optimistic locking on the event stream

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;
}