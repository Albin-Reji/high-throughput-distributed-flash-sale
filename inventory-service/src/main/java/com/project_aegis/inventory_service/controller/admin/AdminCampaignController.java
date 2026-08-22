package com.project_aegis.inventory_service.controller.admin;

import com.project_aegis.inventory_service.dto.campaign.request.FlashCampaignRequest;
import com.project_aegis.inventory_service.dto.campaign.response.CampaignStatusResponse;
import com.project_aegis.inventory_service.dto.campaign.response.FlashCampaignResponse;
import com.project_aegis.inventory_service.dto.response.ApiResponse;
import com.project_aegis.inventory_service.dto.response.PageResponse;
import com.project_aegis.inventory_service.entity.FlashCampaignStatus;
import com.project_aegis.inventory_service.service.FlashCampaignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/inventory/admin/campaigns")
@Tag(name = "Admin Campaigns", description = "Admin APIs for managing flash-sale campaigns and their lifecycle")
public class AdminCampaignController {

    private final FlashCampaignService flashCampaignService;

    // ========================
    // CAMPAIGN CRUD
    // ========================

    @Operation(summary = "Create a new flash campaign in DRAFT status")
    @PostMapping
    public ResponseEntity<ApiResponse<FlashCampaignResponse>> createCampaign(
            @Valid @RequestBody FlashCampaignRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(flashCampaignService.addFlashCampaign(request));
    }

    @Operation(summary = "Get all campaigns with pagination and optional status filter")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<FlashCampaignResponse>>> getAllCampaigns(
            @PageableDefault(
                    size = 20,
                    page = 0,
                    direction = Sort.Direction.ASC
            ) Pageable pageable,
            @RequestParam(required = false) FlashCampaignStatus status) {
        return ResponseEntity.ok(flashCampaignService.getAllFlashCampaign(pageable, status));
    }

    @Operation(summary = "Get campaign by ID with configured SKUs")
    @GetMapping("/{campaignId}")
    public ResponseEntity<ApiResponse<FlashCampaignResponse>> getCampaignById(
            @PathVariable UUID campaignId) {
        return ResponseEntity.ok(flashCampaignService.getCampaignById(campaignId));
    }

    // ========================
    // CAMPAIGN LIFECYCLE
    // ========================

    @Operation(summary = "Publish a DRAFT campaign (transitions to PUBLISHED)")
    @PostMapping("/{campaignId}/publish")
    public ResponseEntity<ApiResponse<CampaignStatusResponse>> publishCampaign(
            @PathVariable UUID campaignId) {
        return ResponseEntity.ok(flashCampaignService.publishCampaign(campaignId));
    }

    @Operation(summary = "Activate a PUBLISHED campaign (transitions to ACTIVE)")
    @PostMapping("/{campaignId}/activate")
    public ResponseEntity<ApiResponse<CampaignStatusResponse>> activateCampaign(
            @PathVariable UUID campaignId) {
        return ResponseEntity.ok(flashCampaignService.activateCampaign(campaignId));
    }

    @Operation(summary = "End an ACTIVE campaign (transitions to ENDED)")
    @PostMapping("/{campaignId}/end")
    public ResponseEntity<ApiResponse<CampaignStatusResponse>> endCampaign(
            @PathVariable UUID campaignId) {
        return ResponseEntity.ok(flashCampaignService.endCampaign(campaignId));
    }
}

