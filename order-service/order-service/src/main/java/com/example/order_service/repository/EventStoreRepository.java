package com.example.order_service.repository;

import com.example.order_service.domain.EventStore;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EventStoreRepository extends JpaRepository<EventStore, Long> {

    // This will be used later for "State Reconstruction" and "Event Replay"
    List<EventStore> findByAggregateIdOrderByVersionAsc(String aggregateId);
}