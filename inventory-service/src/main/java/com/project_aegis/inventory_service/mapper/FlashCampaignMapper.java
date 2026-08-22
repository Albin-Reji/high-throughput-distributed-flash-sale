package com.project_aegis.inventory_service.mapper;

import com.project_aegis.inventory_service.dto.campaign.request.FlashCampaignRequest;
import com.project_aegis.inventory_service.dto.campaign.response.CampaignSkuResponse;
import com.project_aegis.inventory_service.dto.campaign.response.FlashCampaignResponse;
import com.project_aegis.inventory_service.dto.campaign.response.PublicCampaignResponse;
import com.project_aegis.inventory_service.dto.campaign.response.PublicCampaignSkuResponse;
import com.project_aegis.inventory_service.entity.FlashCampaign;
import com.project_aegis.inventory_service.entity.FlashCampaignStatus;
import com.project_aegis.inventory_service.repository.FlashCampaignSkuRepository;
import lombok.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FlashCampaignMapper {

    private final CampaignSkuMapper campaignSkuMapper;
    private  final CampaignSkuResponse campaignSkuResponse;
    private final FlashCampaignSkuRepository flashCampaignSkuRepository;

    public FlashCampaign toEntity(FlashCampaignRequest request) {
        return FlashCampaign.builder()
                .name(request.getName())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(FlashCampaignStatus.DRAFT)
                .build();
    }

    public FlashCampaignResponse toResponse(FlashCampaign campaign) {
        return FlashCampaignResponse.builder()
                .id(campaign.getId())
                .name(campaign.getName())
                .skus(campaign.getFlashCampaignSkus()
                        .stream()
                        .map(campaignSkuResponse::toCampaignSkuResponse)
                        .collect(Collectors.toCollection(ArrayList::new))
                )
                .startTime(campaign.getStartTime())
                .endTime(campaign.getEndTime())
                .status(campaign.getStatus().name())
                .build();
    }

    /**
     * Admin detail response — includes SKU list from the entity's lazy-loaded collection.
     */
    public FlashCampaignResponse toDetailResponse(FlashCampaign campaign) {
        return FlashCampaignResponse.builder()
                .id(campaign.getId())
                .name(campaign.getName())
                .startTime(campaign.getStartTime())
                .endTime(campaign.getEndTime())
                .status(campaign.getStatus().name())
                .skus(campaign.getFlashCampaignSkus().stream()
                        .map(campaignSkuMapper::toResponse)
                        .toList())
                .build();
    }

    /**
     * Public campaign response — no SKUs, used for listing.
     */
    public PublicCampaignResponse toPublicResponse(FlashCampaign campaign) {
        return PublicCampaignResponse.builder()
                .id(campaign.getId())
                .name(campaign.getName())
                .startTime(campaign.getStartTime())
                .endTime(campaign.getEndTime())
                .status(campaign.getStatus().name())
                .build();
    }

    /**
     * Public campaign detail — includes SKUs with live available stock
     * from the inventory system, keyed by skuId.
     */
    public PublicCampaignResponse toPublicDetailResponse(FlashCampaign campaign,
                                                          Map<UUID, Integer> availableStockBySkuId) {
        List<PublicCampaignSkuResponse> skuResponses = campaign.getFlashCampaignSkus().stream()
                .map(sku -> campaignSkuMapper.toPublicResponse(
                        sku,
                        availableStockBySkuId.getOrDefault(sku.getSkuId(), 0)))
                .toList();

        return PublicCampaignResponse.builder()
                .id(campaign.getId())
                .name(campaign.getName())
                .startTime(campaign.getStartTime())
                .endTime(campaign.getEndTime())
                .status(campaign.getStatus().name())
                .skus(skuResponses)
                .build();
    }
}

