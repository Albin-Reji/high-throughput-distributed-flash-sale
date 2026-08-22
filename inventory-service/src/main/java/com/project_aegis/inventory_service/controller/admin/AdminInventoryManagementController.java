package com.project_aegis.inventory_service.controller.admin;

import com.project_aegis.inventory_service.dto.inventory.request.InventoryAdjustRequest;
import com.project_aegis.inventory_service.dto.inventory.request.InventoryInitRequest;
import com.project_aegis.inventory_service.dto.inventory.response.InventoryResponse;
import com.project_aegis.inventory_service.dto.response.ApiResponse;
import com.project_aegis.inventory_service.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/inventory/admin/inventory")
@Tag(name = "Admin Inventory", description = "Admin APIs for initializing, querying, and adjusting actual SKU stock")
public class AdminInventoryManagementController {

    private final InventoryService inventoryService;

    @Operation(summary = "Create and initialize inventory for a SKU")
    @PostMapping
    public ResponseEntity<ApiResponse<InventoryResponse>> initializeInventory(
            @Valid @RequestBody InventoryInitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inventoryService.initializeInventory(request));
    }

    @Operation(summary = "Get inventory level for a SKU (includes total, available, and computed reserved)")
    @GetMapping("/{skuId}")
    public ResponseEntity<ApiResponse<InventoryResponse>> getInventory(
            @PathVariable UUID skuId) {
        return ResponseEntity.ok(inventoryService.getInventory(skuId));
    }

    // newTotal= oldTotal + request.getQuantityDelta
    //ex: newTotal=100 +25 = 125
    //newAvailableQuantity=100 +25 = 125
    @Operation(summary = "Adjust inventory stock delta (positive for restock, negative for damaged/correction)")
    @PatchMapping("/{skuId}")
    public ResponseEntity<ApiResponse<InventoryResponse>> adjustInventory(
            @PathVariable UUID skuId,
            @Valid @RequestBody InventoryAdjustRequest request) {
        return ResponseEntity.ok(inventoryService.adjustInventory(skuId, request));
    }
}
