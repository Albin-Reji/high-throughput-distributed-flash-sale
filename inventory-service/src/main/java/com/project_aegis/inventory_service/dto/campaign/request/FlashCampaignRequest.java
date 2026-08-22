package com.project_aegis.inventory_service.dto.campaign.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlashCampaignRequest {

    @NotBlank
    @Size(max = 255)
    private String name;

    @NotNull
    private Instant startTime;

    @NotNull
    private Instant endTime;
}