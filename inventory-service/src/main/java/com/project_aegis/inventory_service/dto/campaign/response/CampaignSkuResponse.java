package com.project_aegis.inventory_service.dto.campaign.response;

import com.project_aegis.inventory_service.entity.FlashCampaignSku;
import lombok.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Component
public class CampaignSkuResponse {

    private UUID id;

    private UUID campaignId;

    private UUID skuId;

    private BigDecimal flashPrice;

    private Integer allocatedStock;

    private Integer maxPerUser;

    public CampaignSkuResponse toCampaignSkuResponse(FlashCampaignSku sku) {
        return CampaignSkuResponse.builder()
                .id(sku.getId())
                .campaignId(sku.getCampaign()
                        .getId())
                .skuId(sku.getSkuId())
                .flashPrice(sku.getFlashPrice())
                .allocatedStock(sku.getAllocatedStock())
                .maxPerUser(sku.getMaxPerUser())
                .build();
    }
}
