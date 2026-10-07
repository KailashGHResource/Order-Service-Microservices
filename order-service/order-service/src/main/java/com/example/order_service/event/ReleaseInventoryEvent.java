package com.example.order_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReleaseInventoryEvent {
    private Long orderId;
    private List<ItemReleaseDto> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ItemReleaseDto {
        private Long productId;
        private Integer quantity;
    }
}