package com.project_aegis.inventory_service.mapper;

import com.project_aegis.inventory_service.dto.inventory.request.InventoryInitRequest;
import com.project_aegis.inventory_service.dto.inventory.response.InventoryResponse;
import com.project_aegis.inventory_service.entity.Inventory;
import org.springframework.stereotype.Component;

@Component
public class InventoryMapper {

    public Inventory toEntity(InventoryInitRequest request) {
        return Inventory.builder()
                .skuId(request.getSkuId())
                .totalQuantity(request.getTotalQuantity())
                .availableQuantity(request.getTotalQuantity())
                .build();
    }

    /**
     * Maps inventory entity to response with computed reservedQuantity.
     */
    public InventoryResponse toResponse(Inventory inventory) {
        int reserved = inventory.getTotalQuantity() - inventory.getAvailableQuantity();
        return InventoryResponse.builder()
                .skuId(inventory.getSkuId())
                .totalQuantity(inventory.getTotalQuantity())
                .availableQuantity(inventory.getAvailableQuantity())
                .reservedQuantity(reserved)
                .updatedAt(inventory.getUpdatedAt())
                .build();
    }
}
