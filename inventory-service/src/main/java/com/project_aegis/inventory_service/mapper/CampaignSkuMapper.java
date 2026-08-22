package com.project_aegis.inventory_service.mapper;

import com.project_aegis.inventory_service.dto.campaign.request.CampaignSkuRequest;
import com.project_aegis.inventory_service.dto.campaign.response.CampaignSkuResponse;
import com.project_aegis.inventory_service.dto.campaign.response.PublicCampaignSkuResponse;
import com.project_aegis.inventory_service.entity.FlashCampaign;
import com.project_aegis.inventory_service.entity.FlashCampaignSku;
import org.springframework.stereotype.Component;

@Component
public class CampaignSkuMapper {

    public FlashCampaignSku toEntity(CampaignSkuRequest request, FlashCampaign campaign) {
        return FlashCampaignSku.builder()
                .campaign(campaign)
                .skuId(request.getSkuId())
                .flashPrice(request.getFlashPrice())
                .allocatedStock(request.getAllocatedStock())
                .maxPerUser(request.getMaxPerUser() != null ? request.getMaxPerUser() : 10)
                .build();
    }

    public CampaignSkuResponse toResponse(FlashCampaignSku sku) {
        return CampaignSkuResponse.builder()
                .id(sku.getId())
                .campaignId(sku.getCampaign().getId())
                .skuId(sku.getSkuId())
                .flashPrice(sku.getFlashPrice())
                .allocatedStock(sku.getAllocatedStock())
                .maxPerUser(sku.getMaxPerUser())
                .build();
    }

    /**
     * Public-facing response — hides internal campaign SKU id,
     * uses live available stock from the inventory system.
     */
    public PublicCampaignSkuResponse toPublicResponse(FlashCampaignSku sku, Integer availableStock) {
        return PublicCampaignSkuResponse.builder()
                .skuId(sku.getSkuId())
                .flashPrice(sku.getFlashPrice())
                .availableStock(availableStock)
                .maxPerUser(sku.getMaxPerUser())
                .build();
    }
}
