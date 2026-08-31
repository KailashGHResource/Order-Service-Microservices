package com.example.leaveservice.repository;

import com.example.leaveservice.infrastructure.entity.OutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, Long> {

    // Add this line to fix the error
    List<OutboxEventEntity> findByStatusIn(List<OutboxEventEntity.EventStatus> statuses);
}