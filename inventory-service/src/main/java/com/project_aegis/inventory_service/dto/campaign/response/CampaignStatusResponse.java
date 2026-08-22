package com.project_aegis.inventory_service.dto.campaign.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignStatusResponse {

    private UUID id;

    private String status;
}
