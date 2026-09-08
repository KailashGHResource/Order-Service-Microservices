package com.example.leaveservice.application;

import com.example.leaveservice.domain.Leave;
import com.example.leaveservice.dto.LeaveRequestDto;
import com.example.leaveservice.repository.LeaveRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveServiceConcurrencyTest {

    @Mock
    private LeaveRepository leaveRepository;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock rLock;

    @InjectMocks
    private LeaveService leaveService;

    // ==========================================
    // 1. OPTIMISTIC LOCKING TEST (Concurrent Approvals)
    // ==========================================
    @Test
    void givenConcurrentUpdate_whenBulkApprove_thenCatchOptimisticLockingFailure() {
        // Arrange: Create a pending leave at Version 0
        Leave leave = new Leave();
        leave.setId(100L);
        leave.setStatus("PENDING");
        leave.setVersion(0L);

        when(leaveRepository.findById(100L)).thenReturn(Optional.of(leave));

        // Simulate Hibernate throwing the version mismatch exception during save
        // This happens if Thread A and Thread B both read Version 0, but Thread A saves first (making it Version 1).
        // When Thread B tries to save its Version 0, Hibernate throws this exception.
        doThrow(new OptimisticLockingFailureException("Version mismatch detected"))
                .when(leaveRepository).save(any(Leave.class));

        // Act
        Map<String, Object> result = leaveService.bulkApproveLeaves(List.of(100L));

        // Assert: Verify the exception was caught and mapped to a user-friendly error message
        @SuppressWarnings("unchecked")
        List<Long> successful = (List<Long>) result.get("successful");
        @SuppressWarnings("unchecked")
        Map<Long, String> errors = (Map<Long, String>) result.get("errors");

        assertThat(successful).isEmpty();
        assertThat(errors).containsKey(100L);
        assertThat(errors.get(100L)).isEqualTo("Concurrent update detected. Please refresh and try again.");
    }

    // ==========================================
    // 2. DISTRIBUTED LOCKING TEST (Double-Click Prevention)
    // ==========================================
    @Test
    void givenLockHeldByAnotherThread_whenCreateLeave_thenThrowIllegalStateException() throws Exception {
        // Arrange: A user is trying to create a leave request
        LeaveRequestDto requestDto = new LeaveRequestDto();
        requestDto.setEmployeeId(5L);

        when(redissonClient.getLock(anyString())).thenReturn(rLock);

        // Simulate that `tryLock` returns false because another server instance
        // or thread is currently processing a request for this exact employee.
        when(rLock.tryLock(3, 5, TimeUnit.SECONDS)).thenReturn(false);

        // Act & Assert: Verify it fails fast and throws your custom exception
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            leaveService.createLeaveRequest(requestDto);
        });

        assertThat(exception.getMessage())
                .isEqualTo("Another operation is in progress for this employee. Please try again.");

        // Verify that the database save was NEVER called, protecting us from duplicate data
        verify(leaveRepository, never()).save(any());
    }
}