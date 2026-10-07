package com.example.order_service.client;

import com.example.order_service.dto.external.ProductResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service", url = "http://localhost:8086/mock")
public interface ProductClient {

    // Removed /api
    @GetMapping("/products/{skuCode}")
    ProductResponseDto getProductBySku(@PathVariable("skuCode") String skuCode);
}