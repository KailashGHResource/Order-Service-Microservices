package com.example.employeeservice.infrastructure.entity;

import jakarta.persistence.*; // <-- Ensure this is here
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "employees")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;
    private String lastName;

    @Column(unique = true)
    private String email;

    private String password;
    private Long departmentId;

    // ---> ADD THESE TWO LINES <---
    @Version
    private Long version;
}