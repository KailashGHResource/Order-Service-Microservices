package com.example.order_service.outbox;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String aggregateType; // e.g., "ORDER"
    private Long aggregateId;     // e.g., orderId

    @Column(columnDefinition = "TEXT")
    private String payload;       // JSON representation of the event

    private String status;        // "PENDING" or "PROCESSED"

    private LocalDateTime createdAt;
}