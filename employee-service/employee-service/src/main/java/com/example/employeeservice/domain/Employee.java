package com.example.employeeservice.domain;

import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Employee implements Serializable {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private Long departmentId;

    // ---> ADD THIS LINE <---
    private String password;

    @Version
    // ---> ADDED FOR DAY 4: Optimistic Locking support in Domain <---
    private Long version;
}