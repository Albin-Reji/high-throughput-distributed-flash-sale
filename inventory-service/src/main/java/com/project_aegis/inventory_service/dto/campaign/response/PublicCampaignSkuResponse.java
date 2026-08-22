package com.project_aegis.inventory_service.dto.campaign.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicCampaignSkuResponse {

    private UUID skuId;

    private BigDecimal flashPrice;

    private Integer availableStock;

    private Integer maxPerUser;
}
