package com.project_aegis.inventory_service.controller.pub;

import com.project_aegis.inventory_service.dto.campaign.response.PublicCampaignResponse;
import com.project_aegis.inventory_service.dto.campaign.response.PublicSkuAvailabilityResponse;
import com.project_aegis.inventory_service.dto.response.ApiResponse;
import com.project_aegis.inventory_service.entity.FlashCampaignStatus;
import com.project_aegis.inventory_service.service.FlashCampaignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/inventory/campaigns")
@Tag(name = "Public Campaigns", description = "Customer-facing APIs for browsing flash campaigns, stock availability, and SKU flash pricing")
public class PublicCampaignController {

    private final FlashCampaignService flashCampaignService;

    @Operation(summary = "List published or active flash campaigns for customers")
    @GetMapping
    public ResponseEntity<ApiResponse<List<PublicCampaignResponse>>> getPublicCampaigns(
            @RequestParam(required = false) FlashCampaignStatus status) {
        return ResponseEntity.ok(flashCampaignService.getPublicCampaigns(status));
    }

    @Operation(summary = "Get public details of a campaign including SKUs and live stock")
    @GetMapping("/{campaignId}")
    public ResponseEntity<ApiResponse<PublicCampaignResponse>> getPublicCampaignById(
            @PathVariable UUID campaignId) {
        return ResponseEntity.ok(flashCampaignService.getPublicCampaignById(campaignId));
    }

    @Operation(summary = "Get live flash price and available stock for a specific SKU (high-traffic)")
    @GetMapping("/{campaignId}/skus/{skuId}")
    public ResponseEntity<ApiResponse<PublicSkuAvailabilityResponse>> getSkuAvailability(
            @PathVariable UUID campaignId,
            @PathVariable UUID skuId) {
        return ResponseEntity.ok(flashCampaignService.getPublicSkuAvailability(campaignId, skuId));
    }
}
