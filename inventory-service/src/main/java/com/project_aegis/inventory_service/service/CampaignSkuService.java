package com.project_aegis.inventory_service.service;

import com.project_aegis.inventory_service.client.ProductClient;
import com.project_aegis.inventory_service.dto.campaign.request.CampaignSkuRequest;
import com.project_aegis.inventory_service.dto.campaign.request.CampaignSkuUpdateRequest;
import com.project_aegis.inventory_service.dto.campaign.response.CampaignSkuResponse;
import com.project_aegis.inventory_service.dto.response.ApiResponse;
import com.project_aegis.inventory_service.dto.response.SkuResponse;
import com.project_aegis.inventory_service.entity.FlashCampaign;
import com.project_aegis.inventory_service.entity.FlashCampaignSku;
import com.project_aegis.inventory_service.entity.FlashCampaignStatus;
import com.project_aegis.inventory_service.exception.InvalidOperationException;
import com.project_aegis.inventory_service.exception.ResourceNotFoundException;
import com.project_aegis.inventory_service.mapper.CampaignSkuMapper;
import com.project_aegis.inventory_service.repository.FlashCampaignSkuRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignSkuService {

    private final FlashCampaignService flashCampaignService;
    private final FlashCampaignSkuRepository flashCampaignSkuRepository;
    private final CampaignSkuMapper campaignSkuMapper;
    private final ProductClient productClient;

    @Transactional
    public ApiResponse<CampaignSkuResponse> addSkuToCampaign(UUID campaignId, CampaignSkuRequest request) {
        FlashCampaign campaign = flashCampaignService.findCampaignOrThrow(campaignId);
        validateCampaignModifiable(campaign);

        if (flashCampaignSkuRepository.existsByCampaignIdAndSkuId(campaignId, request.getSkuId())) {
            throw new InvalidOperationException(
                    "SKU '" + request.getSkuId() + "' is already added to this campaign");
        }

        // Validate SKU existence via ProductClient if reachable
        try {
            SkuResponse skuResponse = productClient.getSku(request.getSkuId());
            if (skuResponse == null) {
                log.warn("Product service returned null for skuId={}", request.getSkuId());
                throw  new ResourceNotFoundException("Invalid SkuId");
            }
        } catch (Exception e) {
            log.warn("Could not verify SKU with product-service (proceeding): {}", e.getMessage());
            throw  new ResourceNotFoundException("Invalid SkuId: Could not verify SKU with product-service");
        }

        FlashCampaignSku campaignSku = campaignSkuMapper.toEntity(request, campaign);
        flashCampaignSkuRepository.save(campaignSku);

        log.info("Added SKU {} to campaign {}", request.getSkuId(), campaignId);

        return ApiResponse.<CampaignSkuResponse>builder()
                .success(true)
                .message("SKU added to campaign successfully")
                .data(campaignSkuMapper.toResponse(campaignSku))
                .build();
    }

    @Transactional
    public ApiResponse<CampaignSkuResponse> updateCampaignSku(UUID campaignId,
                                                               UUID campaignSkuId,
                                                               CampaignSkuUpdateRequest request) {
        FlashCampaign campaign = flashCampaignService.findCampaignOrThrow(campaignId);
        validateCampaignModifiable(campaign);

        FlashCampaignSku campaignSku = flashCampaignSkuRepository.findById(campaignSkuId)
                .orElseThrow(() -> new ResourceNotFoundException("CampaignSku", "id", campaignSkuId));

        if (!campaignSku.getCampaign().getId().equals(campaignId)) {
            throw new ResourceNotFoundException("CampaignSku", "id", campaignSkuId);
        }

        if (request.getFlashPrice() != null) {
            campaignSku.setFlashPrice(request.getFlashPrice());
        }
        if (request.getAllocatedStock() != null) {
            campaignSku.setAllocatedStock(request.getAllocatedStock());
        }
        if (request.getMaxPerUser() != null) {
            campaignSku.setMaxPerUser(request.getMaxPerUser());
        }

        flashCampaignSkuRepository.save(campaignSku);

        log.info("Updated campaign SKU: campaignSkuId={}, campaignId={}", campaignSkuId, campaignId);

        return ApiResponse.<CampaignSkuResponse>builder()
                .success(true)
                .message("Successfully Updated Campaign Sku")
                .data(campaignSkuMapper.toResponse(campaignSku))
                .build();
    }

    @Transactional
    public ApiResponse<Void> removeSkuFromCampaign(UUID campaignId, UUID campaignSkuId) {
        FlashCampaign campaign = flashCampaignService.findCampaignOrThrow(campaignId);

        if (campaign.getStatus() == FlashCampaignStatus.ACTIVE) {
            throw new InvalidOperationException(
                    "Cannot remove SKU from an ACTIVE campaign");
        }
        if (campaign.getStatus() == FlashCampaignStatus.ENDED) {
            throw new InvalidOperationException(
                    "Cannot remove SKU from an ENDED campaign");
        }

        FlashCampaignSku campaignSku = flashCampaignSkuRepository.findById(campaignSkuId)
                .orElseThrow(() -> new ResourceNotFoundException("CampaignSku", "id", campaignSkuId));

        if (!campaignSku.getCampaign().getId().equals(campaignId)) {
            throw new ResourceNotFoundException("CampaignSku", "id", campaignSkuId);
        }

        flashCampaignSkuRepository.delete(campaignSku);

        log.info("Removed SKU from campaign: campaignSkuId={}, campaignId={}", campaignSkuId, campaignId);

        return ApiResponse.<Void>builder()
                .success(true)
                .message("SKU removed from campaign successfully")
                .build();
    }

    /**
     * Campaign can only be modified while in DRAFT status.
     */
    private void validateCampaignModifiable(FlashCampaign campaign) {
        if (campaign.getStatus() != FlashCampaignStatus.DRAFT) {
            throw new InvalidOperationException(
                    "Campaign can only be modified in DRAFT status. Current status: "
                            + campaign.getStatus().name());
        }
    }
}
