package com.project_aegis.inventory_service.service;

import com.project_aegis.inventory_service.dto.internal.request.ReservationItemRequest;
import com.project_aegis.inventory_service.dto.internal.request.StockDeductRequest;
import com.project_aegis.inventory_service.dto.internal.request.StockReleaseRequest;
import com.project_aegis.inventory_service.dto.internal.request.StockReservationRequest;
import com.project_aegis.inventory_service.dto.internal.response.StockCheckResponse;
import com.project_aegis.inventory_service.dto.internal.response.StockReservationResponse;
import com.project_aegis.inventory_service.dto.response.ApiResponse;
import com.project_aegis.inventory_service.entity.*;
import com.project_aegis.inventory_service.exception.InsufficientStockException;
import com.project_aegis.inventory_service.repository.FlashCampaignRepository;
import com.project_aegis.inventory_service.repository.FlashCampaignSkuRepository;
import com.project_aegis.inventory_service.repository.InventoryRepository;
import com.project_aegis.inventory_service.repository.StockReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InternalInventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private FlashCampaignRepository flashCampaignRepository;

    @Mock
    private FlashCampaignSkuRepository flashCampaignSkuRepository;

    @Mock
    private StockReservationRepository stockReservationRepository;

    @InjectMocks
    private InternalInventoryService internalInventoryService;

    private UUID orderId;
    private UUID skuId;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        skuId = UUID.randomUUID();
        inventory = Inventory.builder()
                .skuId(skuId)
                .totalQuantity(100)
                .availableQuantity(50)
                .build();
    }

    @Test
    @DisplayName("Reserve Stock - Success")
    void reserveStock_Success() {
        StockReservationRequest request = StockReservationRequest.builder()
                .orderId(orderId)
                .items(List.of(ReservationItemRequest.builder()
                        .skuId(skuId)
                        .quantity(5)
                        .build()))
                .build();

        when(stockReservationRepository.findAllByOrderIdAndStatus(orderId, ReservationStatus.RESERVED))
                .thenReturn(Collections.emptyList());
        when(inventoryRepository.findBySkuId(skuId)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
        when(stockReservationRepository.save(any(StockReservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ApiResponse<StockReservationResponse> response = internalInventoryService.reserveStock(request);

        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getData().getStatus()).isEqualTo("RESERVED");
        assertThat(inventory.getAvailableQuantity()).isEqualTo(45);
        verify(stockReservationRepository).save(any(StockReservation.class));
    }

    @Test
    @DisplayName("Reserve Stock - Insufficient quantity throws InsufficientStockException")
    void reserveStock_InsufficientStock_ThrowsException() {
        StockReservationRequest request = StockReservationRequest.builder()
                .orderId(orderId)
                .items(List.of(ReservationItemRequest.builder()
                        .skuId(skuId)
                        .quantity(100)
                        .build()))
                .build();

        when(stockReservationRepository.findAllByOrderIdAndStatus(orderId, ReservationStatus.RESERVED))
                .thenReturn(Collections.emptyList());
        when(inventoryRepository.findBySkuId(skuId)).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> internalInventoryService.reserveStock(request))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    @DisplayName("Release Stock - Restores available quantity and cancels reservation")
    void releaseStock_Success() {
        StockReservation reservation = StockReservation.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .skuId(skuId)
                .quantity(5)
                .status(ReservationStatus.RESERVED)
                .expiresAt(Instant.now().plusSeconds(900))
                .build();

        when(stockReservationRepository.findAllByOrderIdAndStatus(orderId, ReservationStatus.RESERVED))
                .thenReturn(List.of(reservation));
        when(inventoryRepository.findBySkuId(skuId)).thenReturn(Optional.of(inventory));

        ApiResponse<Void> response = internalInventoryService.releaseStock(
                StockReleaseRequest.builder().orderId(orderId).reason("User cancelled").build());

        assertThat(response.getSuccess()).isTrue();
        assertThat(inventory.getAvailableQuantity()).isEqualTo(55);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    @DisplayName("Decrement Stock - Decrements total quantity and commits reservation")
    void decrementStock_Success() {
        StockReservation reservation = StockReservation.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .skuId(skuId)
                .quantity(5)
                .status(ReservationStatus.RESERVED)
                .expiresAt(Instant.now().plusSeconds(900))
                .build();

        when(stockReservationRepository.findAllByOrderIdAndStatus(orderId, ReservationStatus.RESERVED))
                .thenReturn(List.of(reservation));
        when(inventoryRepository.findBySkuId(skuId)).thenReturn(Optional.of(inventory));

        ApiResponse<Void> response = internalInventoryService.decrementStock(
                StockDeductRequest.builder().orderId(orderId).build());

        assertThat(response.getSuccess()).isTrue();
        assertThat(inventory.getTotalQuantity()).isEqualTo(95);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.COMMITTED);
    }

    @Test
    @DisplayName("Check Stock - Success")
    void checkStock_Success() {
        when(inventoryRepository.findBySkuId(skuId)).thenReturn(Optional.of(inventory));

        ApiResponse<StockCheckResponse> response = internalInventoryService.checkStock(skuId);

        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getData().getTotalQuantity()).isEqualTo(100);
        assertThat(response.getData().getAvailableQuantity()).isEqualTo(50);
        assertThat(response.getData().getReservedQuantity()).isEqualTo(50);
        assertThat(response.getData().getInStock()).isTrue();
    }
}
