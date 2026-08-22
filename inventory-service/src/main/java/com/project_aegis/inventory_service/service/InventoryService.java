package com.project_aegis.inventory_service.service;

import com.project_aegis.inventory_service.dto.inventory.request.InventoryAdjustRequest;
import com.project_aegis.inventory_service.dto.inventory.request.InventoryInitRequest;
import com.project_aegis.inventory_service.dto.inventory.response.InventoryResponse;
import com.project_aegis.inventory_service.dto.response.ApiResponse;
import com.project_aegis.inventory_service.entity.Inventory;
import com.project_aegis.inventory_service.exception.InsufficientStockException;
import com.project_aegis.inventory_service.exception.InvalidOperationException;
import com.project_aegis.inventory_service.exception.ResourceNotFoundException;
import com.project_aegis.inventory_service.mapper.InventoryMapper;
import com.project_aegis.inventory_service.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryMapper inventoryMapper;

    @Transactional
    public ApiResponse<InventoryResponse> initializeInventory(InventoryInitRequest request) {
        if (inventoryRepository.existsBySkuId(request.getSkuId())) {
            throw new InvalidOperationException(
                    "Inventory already exists for SKU: " + request.getSkuId());
        }

        Inventory inventory = inventoryMapper.toEntity(request);
        inventoryRepository.save(inventory);

        log.info("Initialized inventory for SKU: skuId={}, totalQuantity={}",
                request.getSkuId(), request.getTotalQuantity());

        return ApiResponse.<InventoryResponse>builder()
                .success(true)
                .message("Inventory initialized successfully")
                .data(inventoryMapper.toResponse(inventory))
                .build();
    }

    @Transactional(readOnly = true)
    public ApiResponse<InventoryResponse> getInventory(UUID skuId) {
        Inventory inventory = findInventoryOrThrow(skuId);

        return ApiResponse.<InventoryResponse>builder()
                .success(true)
                .data(inventoryMapper.toResponse(inventory))
                .build();
    }

    /**
     * Adjusts inventory with optimistic locking via @Version.
     * Positive delta = restock, negative delta = damaged/correction.
     */
    @Transactional
    public ApiResponse<InventoryResponse> adjustInventory(UUID skuId, InventoryAdjustRequest request) {
        Inventory inventory = findInventoryOrThrow(skuId);

        int newTotal = inventory.getTotalQuantity() + request.getQuantityDelta();
        int newAvailable = inventory.getAvailableQuantity() + request.getQuantityDelta();

        if (newTotal < 0) {
            throw new InsufficientStockException(
                    "Adjustment would result in negative total quantity for SKU: " + skuId);
        }
        if (newAvailable < 0) {
            throw new InsufficientStockException(
                    skuId.toString(),
                    Math.abs(request.getQuantityDelta()),
                    inventory.getAvailableQuantity());
        }

        inventory.setTotalQuantity(newTotal);
        inventory.setAvailableQuantity(newAvailable);
        inventoryRepository.save(inventory);

        log.info("Adjusted inventory for SKU: skuId={}, delta={}, reason={}",
                skuId, request.getQuantityDelta(), request.getReason());

        return ApiResponse.<InventoryResponse>builder()
                .success(true)
                .data(inventoryMapper.toResponse(inventory))
                .build();
    }

    private Inventory findInventoryOrThrow(UUID skuId) {
        return inventoryRepository.findBySkuId(skuId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", "skuId", skuId));
    }
}
