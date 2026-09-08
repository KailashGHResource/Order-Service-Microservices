package com.example.leaveservice.infrastructure.repository;

import com.example.leaveservice.infrastructure.entity.LeaveEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class LeaveJpaRepositoryTest {

    @Autowired
    private LeaveJpaRepository leaveJpaRepository;

    private LeaveEntity testLeave;

    @BeforeEach
    void setUp() {
        testLeave = new LeaveEntity();
        testLeave.setEmployeeId(101L);
        testLeave.setStatus("PENDING");
        testLeave.setStartDate(LocalDate.parse("2026-07-01"));
        testLeave.setEndDate(LocalDate.parse("2026-07-05"));
        leaveJpaRepository.save(testLeave);
    }

    @AfterEach
    void tearDown() {
        leaveJpaRepository.deleteAll();
    }

    @Test
    void givenEmployeeId_whenFindByEmployeeId_thenReturnLeaves() {
        // Act
        List<LeaveEntity> leaves = leaveJpaRepository.findByEmployeeId(101L);

        // Assert
        assertThat(leaves).isNotEmpty();
        assertThat(leaves.get(0).getEmployeeId()).isEqualTo(101L);
        assertThat(leaves.get(0).getStatus()).isEqualTo("PENDING");
    }
}