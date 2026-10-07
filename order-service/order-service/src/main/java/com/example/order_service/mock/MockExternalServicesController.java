package com.example.order_service.mock;

import com.example.order_service.dto.external.CustomerResponseDto;
import com.example.order_service.dto.external.InventoryResponseDto;
import com.example.order_service.dto.external.ProductResponseDto;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/mock")
public class MockExternalServicesController {

    // 1. Mock Customer Service
    @GetMapping("/customers/{id}")
    public CustomerResponseDto getCustomer(@PathVariable("id") Long id) {
        return CustomerResponseDto.builder()
                .id(id)
                .name("John Doe")
                .email("john.doe@example.com")
                .build();
    }

    // 2. Mock Product Service
    @GetMapping("/products/{skuCode}")
    public ProductResponseDto getProduct(@PathVariable("skuCode") String skuCode) {
        return ProductResponseDto.builder()
                .skuCode(skuCode)
                .name("Test Product")
                .price(new BigDecimal("100.00")) // $100 per item
                .build();
    }

    // 3. Mock Inventory Service
    @GetMapping("/inventory/check")
    public InventoryResponseDto checkInventory(@RequestParam("skuCode") String skuCode, @RequestParam("quantity") Integer quantity) {
        // For testing, let's pretend SKU "999" is out of stock to test failure, everything else is in stock
        boolean inStock = !skuCode.equals("999");
        return InventoryResponseDto.builder()
                .skuCode(skuCode)
                .isInStock(inStock)
                .availableQuantity(inStock ? 100 : 0)
                .build();
    }
}