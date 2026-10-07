package com.example.order_service.client;

import com.example.order_service.dto.external.CustomerResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "customer-service", url = "http://localhost:8086/mock")
public interface CustomerClient {

    // Removed /api
    @GetMapping("/customers/{id}")
    CustomerResponseDto getCustomerById(@PathVariable("id") Long id);
}