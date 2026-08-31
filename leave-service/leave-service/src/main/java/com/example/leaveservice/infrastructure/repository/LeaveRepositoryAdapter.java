package com.example.leaveservice.infrastructure.repository;

import com.example.leaveservice.domain.Leave;
import com.example.leaveservice.dto.LeaveFilterDto;
import com.example.leaveservice.infrastructure.entity.LeaveEntity;
import com.example.leaveservice.mapper.LeaveMapper;
import com.example.leaveservice.repository.LeaveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class LeaveRepositoryAdapter implements LeaveRepository {

    private final LeaveJpaRepository jpaRepository;
    private final LeaveMapper leaveMapper;

    @Override
    public Leave save(Leave leave) {
        LeaveEntity entity = leaveMapper.toEntity(leave);
        LeaveEntity savedEntity = jpaRepository.save(entity);
        return leaveMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Leave> findById(Long id) {
        return jpaRepository.findById(id).map(leaveMapper::toDomain);
    }

    @Override
    public List<Leave> findAll() {
        return jpaRepository.findAll().stream()
                .map(leaveMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Page<Leave> findWithFilters(LeaveFilterDto filter, Pageable pageable) {
        Specification<LeaveEntity> spec = LeaveSpecification.getFilters(filter);
        Page<LeaveEntity> entityPage = jpaRepository.findAll(spec, pageable);
        return entityPage.map(leaveMapper::toDomain);
    }

    @Override
    public List<Leave> findByEmployeeId(Long employeeId) {
        return jpaRepository.findByEmployeeId(employeeId).stream()
                .map(leaveMapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Leave> saveAll(List<Leave> leaves) {
        List<LeaveEntity> entities = leaves.stream().map(leaveMapper::toEntity).collect(Collectors.toList());
        List<LeaveEntity> savedEntities = jpaRepository.saveAll(entities);
        return savedEntities.stream().map(leaveMapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Leave> findAllById(List<Long> ids) {
        return jpaRepository.findAllById(ids).stream().map(leaveMapper::toDomain).collect(Collectors.toList());
    }
}