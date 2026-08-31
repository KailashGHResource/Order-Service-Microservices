package com.example.leaveservice.application;

import com.example.leaveservice.client.EmployeeClient;
import com.example.leaveservice.dto.EmployeeDto;
import com.example.leaveservice.dto.LeaveFilterDto;
import com.example.leaveservice.dto.LeaveRequestDto;
import com.example.leaveservice.dto.LeaveResponseDto;
import com.example.leaveservice.domain.Leave;
import com.example.leaveservice.infrastructure.entity.OutboxEventEntity;
import com.example.leaveservice.event.LeaveAppliedEvent;
import com.example.leaveservice.mapper.LeaveMapper;
import com.example.leaveservice.repository.LeaveRepository;
import com.example.leaveservice.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveService {

    private final LeaveRepository leaveRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final EmployeeClient employeeClient;
    private final ObjectMapper objectMapper;
    private final RedissonClient redissonClient;
    private final LeaveMapper leaveMapper;

    public LeaveResponseDto createLeaveRequest(LeaveRequestDto requestDto) throws Exception {
        String lockKey = "lock:leave:employee:" + requestDto.getEmployeeId();
        RLock lock = redissonClient.getLock(lockKey);

        boolean isLocked = lock.tryLock(3, 5, TimeUnit.SECONDS);

        if (!isLocked) {
            log.warn("CONCURRENCY TRIGGERED: Could not acquire lock for Employee ID: {}", requestDto.getEmployeeId());
            throw new IllegalStateException("Another operation is in progress for this employee. Please try again.");
        }

        try {
            Leave domainLeave = leaveMapper.toDomain(requestDto);
            return processLeaveCreation(domainLeave);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Transactional
    protected LeaveResponseDto processLeaveCreation(Leave leave) throws Exception {
        List<Leave> existingLeaves = leaveRepository.findByEmployeeId(leave.getEmployeeId());
        boolean isDuplicate = existingLeaves.stream()
                .anyMatch(existing -> "PENDING".equals(existing.getStatus())
                        && existing.getStartDate().equals(leave.getStartDate())
                        && existing.getEndDate().equals(leave.getEndDate()));

        if (isDuplicate) {
            throw new IllegalStateException("Duplicate Request: You already have a pending leave request for these dates.");
        }

        EmployeeDto employee = employeeClient.getEmployeeById(leave.getEmployeeId());

        leave.setStatus("PENDING");
        Leave savedLeave = leaveRepository.save(leave);

        LeaveAppliedEvent event = new LeaveAppliedEvent();
        event.setLeaveId(savedLeave.getId());
        event.setEmployeeId(savedLeave.getEmployeeId());
        event.setStartDate(savedLeave.getStartDate() != null ? savedLeave.getStartDate().toString() : null);
        event.setEndDate(savedLeave.getEndDate() != null ? savedLeave.getEndDate().toString() : null);
        event.setEmployeeEmail(employee != null ? employee.getEmail() : null);

        saveOutboxEvent(savedLeave.getId(), "LEAVE_APPLIED_V1", event);

        return leaveMapper.toDto(savedLeave);
    }

    public Page<Leave> searchLeaves(LeaveFilterDto filter, Pageable pageable) {
        return leaveRepository.findWithFilters(filter, pageable);
    }

    // Day 4: Bulk Leave Approval with Partial Failure Handling & Optimistic Locking
    @Transactional
    public Map<String, Object> bulkApproveLeaves(List<Long> leaveIds) {
        List<Long> successfulIds = new ArrayList<>();
        Map<Long, String> failedIds = new HashMap<>();

        for (Long id : leaveIds) {
            try {
                Leave leave = leaveRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Leave not found"));

                if (!"PENDING".equals(leave.getStatus())) {
                    failedIds.put(id, "Leave is not in PENDING state.");
                    continue;
                }

                leave.setStatus("APPROVED");
                leaveRepository.save(leave); // Triggers optimistic locking check

                saveOutboxEvent(leave.getId(), "LEAVE_APPROVED_V1", Map.of("leaveId", leave.getId(), "status", "APPROVED"));
                successfulIds.add(id);

            } catch (OptimisticLockingFailureException e) {
                log.warn("Optimistic locking failure for leave ID: {}", id);
                failedIds.put(id, "Concurrent update detected. Please refresh and try again.");
            } catch (Exception e) {
                log.error("Failed to approve leave ID: {}", id, e);
                failedIds.put(id, e.getMessage());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("successful", successfulIds);
        result.put("errors", failedIds);
        return result;
    }

    private void saveOutboxEvent(Long aggregateId, String eventType, Object payload) throws Exception {
        OutboxEventEntity outboxEvent = new OutboxEventEntity();
        outboxEvent.setAggregateType("LEAVE_REQUEST");
        outboxEvent.setAggregateId(String.valueOf(aggregateId));
        outboxEvent.setEventType(eventType);
        outboxEvent.setPayload(objectMapper.writeValueAsString(payload));
        outboxEvent.setStatus(OutboxEventEntity.EventStatus.PENDING);
        outboxEventRepository.save(outboxEvent);
    }
}