package com.example.leaveservice.controller;

import com.example.leaveservice.application.LeaveService;
import com.example.leaveservice.domain.Leave;
import com.example.leaveservice.dto.LeaveFilterDto;
import com.example.leaveservice.dto.LeaveRequestDto;
import com.example.leaveservice.dto.LeaveResponseDto;
import com.example.leaveservice.mapper.LeaveMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api") // Base path for the unified controller
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;
    private final LeaveMapper leaveMapper;

    // ==========================================
    //                 V1 APIs
    // ==========================================

    @GetMapping("/v1/leaves/status")
    public String getStatusV1() {
        return "Leave Service V1 is up and running!";
    }

    @PostMapping("/v1/leaves")
    public ResponseEntity<?> createLeaveRequestV1(@RequestBody LeaveRequestDto requestDto) {
        log.info("V1: Received POST request to create leave for Employee ID: {}", requestDto.getEmployeeId());
        try {
            LeaveResponseDto savedLeave = leaveService.createLeaveRequest(requestDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedLeave);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to process leave request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while processing your request.");
        }
    }

    @GetMapping("/v1/leaves/search")
    public ResponseEntity<Page<LeaveResponseDto>> searchLeavesV1(
            @ModelAttribute LeaveFilterDto filter,
            @PageableDefault(page = 0, size = 10, sort = "id") Pageable pageable) {

        log.info("V1: Advanced leave search requested");
        Page<Leave> domainPage = leaveService.searchLeaves(filter, pageable);
        Page<LeaveResponseDto> responsePage = domainPage.map(leaveMapper::toDto);
        return ResponseEntity.ok(responsePage);
    }

    // Day 4: Bulk Operations with 207 Multi-Status
    @PutMapping("/v1/leaves/bulk-approval")
    public ResponseEntity<Map<String, Object>> bulkApproveLeavesV1(@RequestBody List<Long> leaveIds) {
        log.info("V1: Received request for bulk approval of leaves: {}", leaveIds);
        Map<String, Object> result = leaveService.bulkApproveLeaves(leaveIds);

        @SuppressWarnings("unchecked")
        Map<Long, String> errors = (Map<Long, String>) result.get("errors");

        if (!errors.isEmpty()) {
            // Returns 207 Multi-Status if there are partial failures (Optimistic Locking/Not Found)
            return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(result);
        }
        return ResponseEntity.ok(result); // 200 OK if all succeed
    }


    // ==========================================
    //                 V2 APIs
    // ==========================================

    @GetMapping("/v2/leaves/status")
    public String getStatusV2() {
        return "Leave Service V2 is up and running!";
    }

    @GetMapping("/v2/leaves/search")
    public ResponseEntity<Page<LeaveResponseDto>> searchLeavesV2(
            @ModelAttribute LeaveFilterDto filter,
            @PageableDefault(page = 0, size = 10, sort = "id") Pageable pageable) {

        log.info("V2: Advanced leave search requested (Domain-centric)");
        // In a real scenario, V2 might map to a different DTO (e.g., LeaveResponseDtoV2)
        // or apply different default sorting/filtering logic.
        Page<Leave> domainPage = leaveService.searchLeaves(filter, pageable);
        Page<LeaveResponseDto> responsePage = domainPage.map(leaveMapper::toDto);
        return ResponseEntity.ok(responsePage);
    }
}