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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryMapper inventoryMapper;

    @InjectMocks
    private InventoryService inventoryService;

    private UUID skuId;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        skuId = UUID.randomUUID();
        inventory = Inventory.builder()
                .skuId(skuId)
                .totalQuantity(1000)
                .availableQuantity(800)
                .version(1L)
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Initialize Inventory - Success")
    void initializeInventory_Success() {
        InventoryInitRequest request = InventoryInitRequest.builder()
                .skuId(skuId)
                .totalQuantity(2000)
                .build();

        when(inventoryRepository.existsBySkuId(skuId)).thenReturn(false);
        when(inventoryMapper.toEntity(request)).thenReturn(inventory);
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
        when(inventoryMapper.toResponse(inventory)).thenReturn(InventoryResponse.builder()
                .skuId(skuId)
                .totalQuantity(2000)
                .availableQuantity(2000)
                .reservedQuantity(0)
                .build());

        ApiResponse<InventoryResponse> response = inventoryService.initializeInventory(request);

        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getData().getTotalQuantity()).isEqualTo(2000);
        verify(inventoryRepository).save(inventory);
    }

    @Test
    @DisplayName("Initialize Inventory - Already exists throws InvalidOperationException")
    void initializeInventory_AlreadyExists_ThrowsException() {
        InventoryInitRequest request = InventoryInitRequest.builder()
                .skuId(skuId)
                .totalQuantity(2000)
                .build();

        when(inventoryRepository.existsBySkuId(skuId)).thenReturn(true);

        assertThatThrownBy(() -> inventoryService.initializeInventory(request))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Get Inventory - Success")
    void getInventory_Success() {
        when(inventoryRepository.findBySkuId(skuId)).thenReturn(Optional.of(inventory));
        when(inventoryMapper.toResponse(inventory)).thenReturn(InventoryResponse.builder()
                .skuId(skuId)
                .totalQuantity(1000)
                .availableQuantity(800)
                .reservedQuantity(200)
                .build());

        ApiResponse<InventoryResponse> response = inventoryService.getInventory(skuId);

        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getData().getReservedQuantity()).isEqualTo(200);
    }

    @Test
    @DisplayName("Get Inventory - Not Found throws ResourceNotFoundException")
    void getInventory_NotFound_ThrowsException() {
        when(inventoryRepository.findBySkuId(skuId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.getInventory(skuId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Adjust Inventory - Positive delta (restock) succeeds")
    void adjustInventory_PositiveDelta_Success() {
        InventoryAdjustRequest request = InventoryAdjustRequest.builder()
                .quantityDelta(500)
                .reason("WAREHOUSE_RESTOCK")
                .build();

        when(inventoryRepository.findBySkuId(skuId)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
        when(inventoryMapper.toResponse(inventory)).thenReturn(InventoryResponse.builder()
                .skuId(skuId)
                .totalQuantity(1500)
                .availableQuantity(1300)
                .reservedQuantity(200)
                .build());

        ApiResponse<InventoryResponse> response = inventoryService.adjustInventory(skuId, request);

        assertThat(response.getSuccess()).isTrue();
        assertThat(inventory.getTotalQuantity()).isEqualTo(1500);
        assertThat(inventory.getAvailableQuantity()).isEqualTo(1300);
    }

    @Test
    @DisplayName("Adjust Inventory - Negative delta exceeding available stock throws InsufficientStockException")
    void adjustInventory_ExcessiveNegativeDelta_ThrowsException() {
        InventoryAdjustRequest request = InventoryAdjustRequest.builder()
                .quantityDelta(-900)
                .reason("DAMAGED_STOCK")
                .build();

        when(inventoryRepository.findBySkuId(skuId)).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> inventoryService.adjustInventory(skuId, request))
                .isInstanceOf(InsufficientStockException.class);
    }
}
