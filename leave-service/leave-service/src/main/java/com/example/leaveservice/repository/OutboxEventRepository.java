package com.example.leaveservice.repository;

import com.example.leaveservice.infrastructure.entity.OutboxEventEntity;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, Long> {

    // OPTIMIZATION 2: SKIP LOCKED (High-Concurrency Polling)
    // - PESSIMISTIC_WRITE locks the rows so other threads can't grab them.
    // - "-2" is the Hibernate timeout hint for "SKIP LOCKED" (PostgreSQL native feature).
    // Result: Multiple servers can poll the outbox simultaneously without deadlocking or duplicating events!
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2")})
    List<OutboxEventEntity> findByStatusIn(List<OutboxEventEntity.EventStatus> statuses);
}