package com.project_aegis.inventory_service.dto.campaign.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignSkuRequest {

    @NotNull
    private UUID skuId;

    @NotNull
    @Positive
    private BigDecimal flashPrice;

    @NotNull
    @Positive
    private Integer allocatedStock;

    @Positive
    private Integer maxPerUser;
}
