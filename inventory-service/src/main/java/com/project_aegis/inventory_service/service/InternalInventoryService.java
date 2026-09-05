package com.project_aegis.inventory_service.service;

import com.project_aegis.inventory_service.dto.internal.request.ReservationItemRequest;
import com.project_aegis.inventory_service.dto.internal.request.StockDeductRequest;
import com.project_aegis.inventory_service.dto.internal.request.StockReleaseRequest;
import com.project_aegis.inventory_service.dto.internal.request.StockReservationRequest;
import com.project_aegis.inventory_service.dto.internal.response.ReservedItemResponse;
import com.project_aegis.inventory_service.dto.internal.response.StockCheckResponse;
import com.project_aegis.inventory_service.dto.internal.response.StockReservationResponse;
import com.project_aegis.inventory_service.dto.response.ApiResponse;
import com.project_aegis.inventory_service.entity.*;
import com.project_aegis.inventory_service.exception.InsufficientStockException;
import com.project_aegis.inventory_service.exception.InvalidOperationException;
import com.project_aegis.inventory_service.exception.ResourceNotFoundException;
import com.project_aegis.inventory_service.repository.FlashCampaignRepository;
import com.project_aegis.inventory_service.repository.FlashCampaignSkuRepository;
import com.project_aegis.inventory_service.repository.InventoryRepository;
import com.project_aegis.inventory_service.repository.StockReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InternalInventoryService {

    private final InventoryRepository inventoryRepository;
    private final FlashCampaignRepository flashCampaignRepository;
    private final FlashCampaignSkuRepository flashCampaignSkuRepository;
    private final StockReservationRepository stockReservationRepository;

    /**
     * Atomically reserves stock for an order.
     * Supports idempotency: returns existing reservation if already processed.
     */
    @Transactional
    public ApiResponse<StockReservationResponse> reserveStock(StockReservationRequest request) {
        log.info("Processing stock reservation for orderId={}, customerId={}, campaignId={}",
                request.getOrderId(), request.getCustomerId(), request.getCampaignId());

        // Idempotency check: if reservations exist for this orderId in RESERVED state, return them
        List<StockReservation> existingReservations = stockReservationRepository
                .findAllByOrderIdAndStatus(request.getOrderId(), ReservationStatus.RESERVED);

        if (!existingReservations.isEmpty()) {
            log.info("Idempotent hit: Returning existing reservations for orderId={}", request.getOrderId());
            return buildReservationResponse(request.getOrderId(), request.getCustomerId(),
                    request.getCampaignId(), existingReservations);
        }



        // Validate campaign if specified
        final FlashCampaign campaign;
        if (request.getCampaignId() != null) {
            FlashCampaign found = flashCampaignRepository.findById(request.getCampaignId())
                    .orElseThrow(() -> new ResourceNotFoundException("Campaign", "id", request.getCampaignId()));

            if (found.getStatus() != FlashCampaignStatus.ACTIVE) {
                throw new InvalidOperationException("Flash campaign is not currently ACTIVE. Status: "
                        + found.getStatus());
            }
            campaign = found;
        } else {
            campaign = null;
        }

        Instant expiresAt = Instant.now().plusSeconds(
                request.getTimeoutSeconds() != null ? request.getTimeoutSeconds() : 900);

        List<StockReservation> createdReservations = new ArrayList<>();
        List<ReservedItemResponse> itemResponses = new ArrayList<>();

        for (ReservationItemRequest itemReq : request.getItems()) {
            Inventory inventory = inventoryRepository.findBySkuId(itemReq.getSkuId())
                    .orElseThrow(() -> new ResourceNotFoundException("Inventory", "skuId", itemReq.getSkuId()));

            BigDecimal itemPrice = null;

            // Flash sale SKU validation
            if (campaign != null) {
                final UUID activeCampaignId = campaign.getId();
                FlashCampaignSku campaignSku = flashCampaignSkuRepository
                        .findByCampaignIdAndSkuId(activeCampaignId, itemReq.getSkuId())
                        .orElseThrow(() -> new InvalidOperationException("SKU " + itemReq.getSkuId()
                                + " is not part of campaign " + activeCampaignId));

                if (itemReq.getQuantity() > campaignSku.getMaxPerUser()) {
                    throw new InvalidOperationException(String.format(
                            "Requested quantity %d exceeds max allowed per user %d for SKU %s",
                            itemReq.getQuantity(), campaignSku.getMaxPerUser(), itemReq.getSkuId()));
                }

                itemPrice = campaignSku.getFlashPrice();
            }

            int updatedRows = inventoryRepository.reserveStock(itemReq.getSkuId(), itemReq.getQuantity());
            if(updatedRows == 0) {
                int currentAvailableQuantity = inventoryRepository
                        .findBySkuId(itemReq.getSkuId())
                        .map(Inventory::getAvailableQuantity)
                        .orElse(0);

                log.warn("Insufficient stock for SKU {}: requested {}, available {}",
                        itemReq.getSkuId(), itemReq.getQuantity(), currentAvailableQuantity);

                throw new InsufficientStockException(
                        itemReq.getSkuId().toString(),
                        itemReq.getQuantity(),
                        currentAvailableQuantity);
            }


            StockReservation reservation = StockReservation.builder()
                    .orderId(request.getOrderId())
                    .customerId(request.getCustomerId())
                    .campaignId(request.getCampaignId())
                    .skuId(itemReq.getSkuId())
                    .quantity(itemReq.getQuantity())
                    .status(ReservationStatus.RESERVED)
                    .expiresAt(expiresAt)
                    .build();

            stockReservationRepository.save(reservation);
            createdReservations.add(reservation);

            itemResponses.add(ReservedItemResponse.builder()
                    .skuId(itemReq.getSkuId())
                    .quantity(itemReq.getQuantity())
                    .flashPrice(itemPrice)
                    .build());
        }

        log.info("Successfully reserved stock for orderId={}, items count={}",
                request.getOrderId(), createdReservations.size());

        return ApiResponse.<StockReservationResponse>builder()
                .success(true)
                .message("Stock reserved successfully")
                .data(StockReservationResponse.builder()
                        .orderId(request.getOrderId())
                        .customerId(request.getCustomerId())
                        .campaignId(request.getCampaignId())
                        .status(ReservationStatus.RESERVED.name())
                        .expiresAt(expiresAt)
                        .items(itemResponses)
                        .build())
                .build();
    }

    /**
     * Releases reserved stock upon order cancellation or timeout.
     */
    @Transactional
    public ApiResponse<Void> releaseStock(StockReleaseRequest request) {
        log.info("Releasing stock reservations for orderId={}, reason={}",
                request.getOrderId(), request.getReason());

        List<StockReservation> reservations = stockReservationRepository
                .findAllByOrderIdAndStatus(request.getOrderId(), ReservationStatus.RESERVED);

        if (reservations.isEmpty()) {
            log.warn("No active reservations found to release for orderId={}", request.getOrderId());
            return ApiResponse.<Void>builder()
                    .success(true)
                    .message("No active reservations found to release")
                    .build();
        }

        for (StockReservation reservation : reservations) {
            Optional<Inventory> inventoryOpt = inventoryRepository.findBySkuId(reservation.getSkuId());
            if (inventoryOpt.isPresent()) {
                Inventory inventory = inventoryOpt.get();
                inventory.setAvailableQuantity(inventory.getAvailableQuantity() + reservation.getQuantity());
                inventoryRepository.save(inventory);
            }

            reservation.setStatus(ReservationStatus.CANCELLED);
            stockReservationRepository.save(reservation);
        }

        log.info("Successfully released {} stock reservation(s) for orderId={}",
                reservations.size(), request.getOrderId());

        return ApiResponse.<Void>builder()
                .success(true)
                .message("Stock reservations released successfully")
                .build();
    }

    /**
     * Confirms and decrements reserved stock upon successful order payment.
     */
    @Transactional
    public ApiResponse<Void> decrementStock(StockDeductRequest request) {
        log.info("Confirming/decrementing stock for orderId={}", request.getOrderId());

        List<StockReservation> reservations = stockReservationRepository
                .findAllByOrderIdAndStatus(request.getOrderId(), ReservationStatus.RESERVED);

        if (reservations.isEmpty()) {
            throw new ResourceNotFoundException("No active stock reservations found for orderId: " + request.getOrderId());
        }

        for (StockReservation reservation : reservations) {
            Inventory inventory = inventoryRepository.findBySkuId(reservation.getSkuId())
                    .orElseThrow(() -> new ResourceNotFoundException("Inventory", "skuId", reservation.getSkuId()));

            // Decrement total quantity (available was already reduced during reservation)
            inventory.setTotalQuantity(inventory.getTotalQuantity() - reservation.getQuantity());
            inventoryRepository.save(inventory);

            reservation.setStatus(ReservationStatus.COMMITTED);
            stockReservationRepository.save(reservation);
        }

        log.info("Successfully committed and decremented stock for orderId={}", request.getOrderId());

        return ApiResponse.<Void>builder()
                .success(true)
                .message("Stock decremented and committed successfully")
                .build();
    }

    /**
     * Checks current stock availability for a SKU.
     */
    @Transactional(readOnly = true)
    public ApiResponse<StockCheckResponse> checkStock(UUID skuId) {
        Inventory inventory = inventoryRepository.findBySkuId(skuId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", "skuId", skuId));

        int reserved = inventory.getTotalQuantity() - inventory.getAvailableQuantity();

        return ApiResponse.<StockCheckResponse>builder()
                .success(true)
                .data(StockCheckResponse.builder()
                        .skuId(skuId)
                        .totalQuantity(inventory.getTotalQuantity())
                        .availableQuantity(inventory.getAvailableQuantity())
                        .reservedQuantity(reserved)
                        .inStock(inventory.getAvailableQuantity() > 0)
                        .build())
                .build();
    }

    private ApiResponse<StockReservationResponse> buildReservationResponse(
            UUID orderId, UUID customerId, UUID campaignId, List<StockReservation> reservations) {

        Instant expiresAt = reservations.isEmpty() ? Instant.now() : reservations.get(0).getExpiresAt();

        List<ReservedItemResponse> items = reservations.stream()
                .map(r -> ReservedItemResponse.builder()
                        .skuId(r.getSkuId())
                        .quantity(r.getQuantity())
                        .build())
                .toList();

        return ApiResponse.<StockReservationResponse>builder()
                .success(true)
                .message("Stock reservation already exists")
                .data(StockReservationResponse.builder()
                        .orderId(orderId)
                        .customerId(customerId)
                        .campaignId(campaignId)
                        .status(ReservationStatus.RESERVED.name())
                        .expiresAt(expiresAt)
                        .items(items)
                        .build())
                .build();
    }
}
