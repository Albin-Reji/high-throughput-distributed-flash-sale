package com.project_aegis.inventory_service.dto.campaign.request;

import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignSkuUpdateRequest {

    @Positive
    private BigDecimal flashPrice;

    @Positive
    private Integer allocatedStock;

    @Positive
    private Integer maxPerUser;
}
