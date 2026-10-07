package com.example.order_service.client;

import com.example.order_service.dto.external.InventoryResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "inventory-service", url = "http://localhost:8086/mock")
public interface InventoryClient {

    // Removed /api
    @GetMapping("/inventory/check")
    InventoryResponseDto checkInventory(
            @RequestParam("skuCode") String skuCode,
            @RequestParam("quantity") Integer quantity
    );
}