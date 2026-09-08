package com.example.leaveservice.application;

import com.example.leaveservice.client.EmployeeClient;
import com.example.leaveservice.domain.Leave;
import com.example.leaveservice.dto.EmployeeDto;
import com.example.leaveservice.dto.LeaveRequestDto;
import com.example.leaveservice.dto.LeaveResponseDto;
import com.example.leaveservice.infrastructure.entity.OutboxEventEntity;
import com.example.leaveservice.mapper.LeaveMapper;
import com.example.leaveservice.repository.LeaveRepository;
import com.example.leaveservice.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // <--- Prevents UnnecessaryStubbingException across differing test paths
class LeaveServiceTest {

    @Mock
    private LeaveRepository leaveRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private EmployeeClient employeeClient;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock rLock;

    @Mock
    private LeaveMapper leaveMapper;

    @InjectMocks
    private LeaveService leaveService;

    private LeaveRequestDto requestDto;
    private Leave domainLeave;
    private Leave savedLeave;
    private LeaveResponseDto responseDto;
    private EmployeeDto employeeDto;

    @BeforeEach
    void setUp() throws Exception {
        // Setup general ObjectMapper mock for outbox event serialization
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"mockedJson\":\"payload\"}");

        // Setup Test DTOs and Entities
        requestDto = new LeaveRequestDto();
        requestDto.setEmployeeId(1L);
        requestDto.setStartDate(LocalDate.of(2026, 6, 1));
        requestDto.setEndDate(LocalDate.of(2026, 6, 5));

        domainLeave = new Leave();
        domainLeave.setEmployeeId(1L);
        domainLeave.setStatus("PENDING");
        domainLeave.setStartDate(LocalDate.of(2026, 6, 1));
        domainLeave.setEndDate(LocalDate.of(2026, 6, 5));

        savedLeave = new Leave();
        savedLeave.setId(10L);
        savedLeave.setEmployeeId(1L);
        savedLeave.setStatus("PENDING");
        savedLeave.setStartDate(LocalDate.of(2026, 6, 1));
        savedLeave.setEndDate(LocalDate.of(2026, 6, 5));

        responseDto = LeaveResponseDto.builder()
                .id(10L)
                .status("PENDING")
                .build();

        employeeDto = new EmployeeDto();
        employeeDto.setId(1L);
        employeeDto.setEmail("clark.kent@dailyplanet.com");
    }

    @Test
    void givenValidRequest_whenCreateLeaveRequest_thenSuccess() throws Exception {
        // Arrange
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);

        when(leaveMapper.toDomain(requestDto)).thenReturn(domainLeave);
        when(leaveRepository.findByEmployeeId(1L)).thenReturn(Collections.emptyList());
        when(employeeClient.getEmployeeById(1L)).thenReturn(employeeDto);
        when(leaveRepository.save(any(Leave.class))).thenReturn(savedLeave);
        when(leaveMapper.toDto(savedLeave)).thenReturn(responseDto);

        // Act
        LeaveResponseDto result = leaveService.createLeaveRequest(requestDto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10L);

        verify(leaveRepository, times(1)).save(any(Leave.class));
        verify(outboxEventRepository, times(1)).save(any(OutboxEventEntity.class));
        verify(rLock, times(1)).unlock();
    }

    @Test
    void givenConcurrentRequest_whenLockFails_thenThrowException() throws Exception {
        // Arrange
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> leaveService.createLeaveRequest(requestDto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Another operation is in progress for this employee");

        verify(leaveRepository, never()).save(any(Leave.class));
        verify(outboxEventRepository, never()).save(any(OutboxEventEntity.class));
    }

    @Test
    void givenExistingPendingLeave_whenCreateLeaveRequest_thenThrowDuplicateException() throws Exception {
        // Arrange
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);

        Leave existingLeave = new Leave();
        existingLeave.setStatus("PENDING");
        existingLeave.setStartDate(LocalDate.of(2026, 6, 1));
        existingLeave.setEndDate(LocalDate.of(2026, 6, 5));

        when(leaveMapper.toDomain(requestDto)).thenReturn(domainLeave);
        when(leaveRepository.findByEmployeeId(1L)).thenReturn(List.of(existingLeave));

        // Act & Assert
        assertThatThrownBy(() -> leaveService.createLeaveRequest(requestDto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate Request: You already have a pending leave request");

        verify(leaveRepository, never()).save(any(Leave.class));
    }

    @Test
    void givenPendingLeave_whenBulkApproveLeaves_thenSuccess() {
        // Arrange
        Leave pendingLeave = new Leave();
        pendingLeave.setId(100L);
        pendingLeave.setStatus("PENDING");

        when(leaveRepository.findById(100L)).thenReturn(Optional.of(pendingLeave));
        when(leaveRepository.save(any(Leave.class))).thenReturn(pendingLeave);

        // Act
        Map<String, Object> result = leaveService.bulkApproveLeaves(List.of(100L));

        // Assert
        assertThat(result).isNotNull();

        List<Long> successful = (List<Long>) result.get("successful");
        assertThat(successful).contains(100L);

        verify(leaveRepository, times(1)).save(pendingLeave);
        verify(outboxEventRepository, times(1)).save(any(OutboxEventEntity.class));
    }
}