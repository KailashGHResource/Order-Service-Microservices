package com.example.order_service.repository;

import com.example.order_service.domain.Order;
import com.example.order_service.domain.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Finds orders in a specific status that were created before a certain time
    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.createdAt < :thresholdTime")
    List<Order> findStuckOrders(@Param("status") OrderStatus status, @Param("thresholdTime") LocalDateTime thresholdTime);

    // NEW: Adds support for filtering by status with pagination
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
}