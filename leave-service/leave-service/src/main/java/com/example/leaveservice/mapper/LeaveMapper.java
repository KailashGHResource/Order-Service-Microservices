package com.example.leaveservice.mapper;

import com.example.leaveservice.domain.Leave;
import com.example.leaveservice.dto.LeaveRequestDto;
import com.example.leaveservice.dto.LeaveResponseDto;
import com.example.leaveservice.infrastructure.entity.LeaveEntity;
import org.springframework.stereotype.Component;

@Component
public class LeaveMapper {

    public Leave toDomain(LeaveEntity entity) {
        if (entity == null) return null;
        return Leave.builder()
                .id(entity.getId())
                .employeeId(entity.getEmployeeId())
                .leaveType(entity.getLeaveType())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .status(entity.getStatus())
                .version(entity.getVersion()) // Mapped Version
                .build();
    }

    public LeaveEntity toEntity(Leave domain) {
        if (domain == null) return null;
        return LeaveEntity.builder()
                .id(domain.getId())
                .employeeId(domain.getEmployeeId())
                .leaveType(domain.getLeaveType())
                .startDate(domain.getStartDate())
                .endDate(domain.getEndDate())
                .status(domain.getStatus())
                .version(domain.getVersion()) // Mapped Version
                .build();
    }

    public Leave toDomain(LeaveRequestDto dto) {
        if (dto == null) return null;
        return Leave.builder()
                .employeeId(dto.getEmployeeId())
                .leaveType(dto.getLeaveType())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .status("PENDING")
                .build();
    }

    public LeaveResponseDto toDto(Leave domain) {
        if (domain == null) return null;
        return LeaveResponseDto.builder()
                .id(domain.getId())
                .employeeId(domain.getEmployeeId())
                .leaveType(domain.getLeaveType())
                .startDate(domain.getStartDate())
                .endDate(domain.getEndDate())
                .status(domain.getStatus())
                .build();
    }
}