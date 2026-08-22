package com.project_aegis.inventory_service.dto.campaign.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicSkuAvailabilityResponse {

    private UUID campaignId;

    private UUID skuId;

    private BigDecimal flashPrice;

    private Integer availableStock;

    private Integer maxPerUser;

    private String campaignStatus;
}
