package com.example.auditservice.controller;

import com.example.auditservice.application.AuditService;
import com.example.auditservice.dto.AuditResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/audit") // Adopting the V1 versioning standard
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/status")
    public String getStatus() {
        log.info("Received request to check Audit Service status");
        return "Audit Service V1 is up and running on port 8085!";
    }

    // Endpoint to view all captured audit logs safely using DTOs
    @GetMapping
    public ResponseEntity<List<AuditResponseDto>> getAllAuditLogs() {
        log.info("REST request to fetch all audit logs");
        List<AuditResponseDto> logs = auditService.getAllAuditLogs();
        return ResponseEntity.ok(logs);
    }
}