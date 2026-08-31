package com.example.leaveservice.infrastructure.entity;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_events")
@Data
public class OutboxEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String aggregateType;
    private String aggregateId;
    private String eventType;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    private EventStatus status;

    private int retryCount = 0;
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum EventStatus {
        PENDING, PROCESSED, FAILED
    }
}