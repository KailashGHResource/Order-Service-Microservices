package com.example.order_service.dto.external;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerResponseDto implements Serializable {
    private Long id;
    private String name;
    private String email;
    private String status; // e.g., "ACTIVE", "INACTIVE"
}