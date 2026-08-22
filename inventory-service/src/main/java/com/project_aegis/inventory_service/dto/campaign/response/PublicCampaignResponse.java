package com.project_aegis.inventory_service.dto.campaign.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PublicCampaignResponse {

    private UUID id;

    private String name;

    private Instant startTime;

    private Instant endTime;

    private String status;

    private List<PublicCampaignSkuResponse> skus;
}
