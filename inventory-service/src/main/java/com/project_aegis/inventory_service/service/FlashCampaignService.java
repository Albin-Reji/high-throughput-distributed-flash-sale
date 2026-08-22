package com.project_aegis.inventory_service.service;

import com.project_aegis.inventory_service.dto.campaign.request.FlashCampaignRequest;
import com.project_aegis.inventory_service.dto.campaign.response.*;
import com.project_aegis.inventory_service.dto.response.ApiResponse;
import com.project_aegis.inventory_service.dto.response.PageResponse;
import com.project_aegis.inventory_service.entity.FlashCampaign;
import com.project_aegis.inventory_service.entity.FlashCampaignSku;
import com.project_aegis.inventory_service.entity.FlashCampaignStatus;
import com.project_aegis.inventory_service.entity.Inventory;
import com.project_aegis.inventory_service.exception.InvalidOperationException;
import com.project_aegis.inventory_service.exception.InvalidStateTransitionException;
import com.project_aegis.inventory_service.exception.ResourceNotFoundException;
import com.project_aegis.inventory_service.mapper.CampaignSkuMapper;
import com.project_aegis.inventory_service.mapper.FlashCampaignMapper;
import com.project_aegis.inventory_service.repository.FlashCampaignRepository;
import com.project_aegis.inventory_service.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlashCampaignService {

    private final FlashCampaignMapper flashCampaignMapper;
    private final CampaignSkuMapper campaignSkuMapper;
    private final FlashCampaignRepository flashCampaignRepository;
    private final InventoryRepository inventoryRepository;

    // ========================
    // ADMIN APIs
    // ========================

    @Transactional
    public ApiResponse<FlashCampaignResponse> addFlashCampaign(FlashCampaignRequest request) {
        if (flashCampaignRepository.existsByName(request.getName())) {
            throw new InvalidOperationException(
                    "Campaign with name '" + request.getName() + "' already exists");
        }

        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new InvalidOperationException("End time must be after start time");
        }

        FlashCampaign campaign = flashCampaignMapper.toEntity(request);
        flashCampaignRepository.save(campaign);

        log.info("Created flash campaign: id={}, name={}", campaign.getId(), campaign.getName());

        return ApiResponse.<FlashCampaignResponse>builder()
                .success(true)
                .message("Campaign created successfully")
                .data(flashCampaignMapper.toResponse(campaign))
                .build();
    }

    @Transactional(readOnly = true)
    public ApiResponse<PageResponse<FlashCampaignResponse>> getAllFlashCampaign(Pageable pageable,
                                                                                FlashCampaignStatus status) {
        Page<FlashCampaign> campaignPage;
        if (status != null) {
            campaignPage = flashCampaignRepository.findAllByStatus(pageable, status);
        } else {
            campaignPage = flashCampaignRepository.findAll(pageable);
        }

        PageResponse<FlashCampaignResponse> response = PageResponse.<FlashCampaignResponse>builder()
                .content(campaignPage.stream()
                        .map(flashCampaignMapper::toResponse)
                        .collect(Collectors.toCollection(ArrayList::new)))
                .first(campaignPage.isFirst())
                .last(campaignPage.isLast())
                .page(campaignPage.getNumber())
                .size(campaignPage.getSize())
                .totalElements(campaignPage.getTotalElements())
                .totalPages(campaignPage.getTotalPages())
                .build();

        return ApiResponse.<PageResponse<FlashCampaignResponse>>builder()
                .success(true)
                .data(response)
                .build();
    }


    @Transactional(readOnly = true)
    public ApiResponse<FlashCampaignResponse> getCampaignById(UUID campaignId) {
        FlashCampaign campaign = findCampaignOrThrow(campaignId);
        return ApiResponse.<FlashCampaignResponse>builder()
                .success(true)
                .data(flashCampaignMapper.toDetailResponse(campaign))
                .build();
    }

    // ========================
    // LIFECYCLE APIs
    // ========================

    @Transactional
    public ApiResponse<CampaignStatusResponse> publishCampaign(UUID campaignId) {
        FlashCampaign campaign = findCampaignOrThrow(campaignId);
        validateStateTransition(campaign, FlashCampaignStatus.DRAFT, FlashCampaignStatus.PUBLISHED);

        if (campaign.getFlashCampaignSkus().isEmpty()) {
            throw new InvalidOperationException(
                    "Cannot publish campaign without any SKUs configured");
        }

        campaign.setStatus(FlashCampaignStatus.PUBLISHED);
        flashCampaignRepository.save(campaign);

        log.info("Published campaign: id={}", campaignId);

        return ApiResponse.<CampaignStatusResponse>builder()
                .success(true)
                .message("Campaign published successfully")
                .data(CampaignStatusResponse.builder()
                        .id(campaign.getId())
                        .status(campaign.getStatus().name())
                        .build())
                .build();
    }

    @Transactional
    public ApiResponse<CampaignStatusResponse> activateCampaign(UUID campaignId) {
        FlashCampaign campaign = findCampaignOrThrow(campaignId);
        validateStateTransition(campaign, FlashCampaignStatus.PUBLISHED, FlashCampaignStatus.ACTIVE);

        campaign.setStatus(FlashCampaignStatus.ACTIVE);
        flashCampaignRepository.save(campaign);

        log.info("Activated campaign: id={}", campaignId);

        return ApiResponse.<CampaignStatusResponse>builder()
                .success(true)
                .message("Campaign activated successfully")
                .data(CampaignStatusResponse.builder()
                        .id(campaign.getId())
                        .status(campaign.getStatus().name())
                        .build())
                .build();
    }

    @Transactional
    public ApiResponse<CampaignStatusResponse> endCampaign(UUID campaignId) {
        FlashCampaign campaign = findCampaignOrThrow(campaignId);
        validateStateTransition(campaign, FlashCampaignStatus.ACTIVE, FlashCampaignStatus.ENDED);

        campaign.setStatus(FlashCampaignStatus.ENDED);
        flashCampaignRepository.save(campaign);

        log.info("Ended campaign: id={}", campaignId);

        return ApiResponse.<CampaignStatusResponse>builder()
                .success(true)
                .message("Campaign ended successfully")
                .data(CampaignStatusResponse.builder()
                        .id(campaign.getId())
                        .status(campaign.getStatus().name())
                        .build())
                .build();
    }

    // ========================
    // PUBLIC APIs
    // ========================

    @Transactional(readOnly = true)
    public ApiResponse<List<PublicCampaignResponse>> getPublicCampaigns(FlashCampaignStatus status) {
        List<FlashCampaignStatus> allowedStatuses;
        if (status != null) {
            // Only allow PUBLISHED and ACTIVE for public API
            if (status != FlashCampaignStatus.PUBLISHED && status != FlashCampaignStatus.ACTIVE) {
                throw new InvalidOperationException(
                        "Public API only supports PUBLISHED or ACTIVE status filter");
            }
            allowedStatuses = List.of(status);
        } else {
            allowedStatuses = List.of(FlashCampaignStatus.PUBLISHED, FlashCampaignStatus.ACTIVE);
        }

        List<PublicCampaignResponse> campaigns = flashCampaignRepository
                .findAllByStatusIn(allowedStatuses,
                        Pageable.unpaged())
                .stream()
                .map(flashCampaignMapper::toPublicResponse)
                .toList();

        return ApiResponse.<List<PublicCampaignResponse>>builder()
                .success(true)
                .data(campaigns)
                .build();
    }

    @Transactional(readOnly = true)
    public ApiResponse<PublicCampaignResponse> getPublicCampaignById(UUID campaignId) {
        FlashCampaign campaign = findCampaignOrThrow(campaignId);

        // Only show PUBLISHED or ACTIVE campaigns publicly
        if (campaign.getStatus() != FlashCampaignStatus.PUBLISHED
                && campaign.getStatus() != FlashCampaignStatus.ACTIVE) {
            throw new ResourceNotFoundException("Campaign", "id", campaignId);
        }

        // Build available stock map from inventory
        Map<UUID, Integer> availableStockMap = buildAvailableStockMap(campaign.getFlashCampaignSkus());

        return ApiResponse.<PublicCampaignResponse>builder()
                .success(true)
                .data(flashCampaignMapper.toPublicDetailResponse(campaign, availableStockMap))
                .build();
    }

    @Transactional(readOnly = true)
    public ApiResponse<PublicSkuAvailabilityResponse> getPublicSkuAvailability(UUID campaignId, UUID skuId) {
        FlashCampaign campaign = findCampaignOrThrow(campaignId);

        if (campaign.getStatus() != FlashCampaignStatus.PUBLISHED
                && campaign.getStatus() != FlashCampaignStatus.ACTIVE) {
            throw new ResourceNotFoundException("Campaign", "id", campaignId);
        }

        FlashCampaignSku campaignSku = campaign.getFlashCampaignSkus().stream()
                .filter(s -> s.getSkuId().equals(skuId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("SKU", "skuId", skuId));

        int availableStock = inventoryRepository.findBySkuId(skuId)
                .map(Inventory::getAvailableQuantity)
                .orElse(0);

        return ApiResponse.<PublicSkuAvailabilityResponse>builder()
                .success(true)
                .data(PublicSkuAvailabilityResponse.builder()
                        .campaignId(campaignId)
                        .skuId(skuId)
                        .flashPrice(campaignSku.getFlashPrice())
                        .availableStock(availableStock)
                        .maxPerUser(campaignSku.getMaxPerUser())
                        .campaignStatus(campaign.getStatus().name())
                        .build())
                .build();
    }

    // ========================
    // HELPER METHODS
    // ========================
    // find campaign by campaign Id
    //or Else Throw
    public FlashCampaign findCampaignOrThrow(UUID campaignId) {
        return flashCampaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign", "id", campaignId));
    }


    private void validateStateTransition(FlashCampaign campaign,
                                          FlashCampaignStatus requiredCurrent,
                                          FlashCampaignStatus target) {
        if (campaign.getStatus() != requiredCurrent) {
            throw new InvalidStateTransitionException(
                    campaign.getStatus().name(), target.name());
        }
    }

    private Map<UUID, Integer> buildAvailableStockMap(List<FlashCampaignSku> skus) {
        Map<UUID, Integer> stockMap = new HashMap<>();
        for (FlashCampaignSku sku : skus) {
            int available = inventoryRepository.findBySkuId(sku.getSkuId())
                    .map(Inventory::getAvailableQuantity)
                    .orElse(0);
            stockMap.put(sku.getSkuId(), available);
        }
        return stockMap;
    }
}

