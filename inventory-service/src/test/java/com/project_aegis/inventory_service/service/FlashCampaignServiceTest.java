package com.project_aegis.inventory_service.service;

import com.project_aegis.inventory_service.dto.campaign.request.FlashCampaignRequest;
import com.project_aegis.inventory_service.dto.campaign.response.CampaignStatusResponse;
import com.project_aegis.inventory_service.dto.campaign.response.FlashCampaignResponse;
import com.project_aegis.inventory_service.dto.response.ApiResponse;
import com.project_aegis.inventory_service.entity.FlashCampaign;
import com.project_aegis.inventory_service.entity.FlashCampaignSku;
import com.project_aegis.inventory_service.entity.FlashCampaignStatus;
import com.project_aegis.inventory_service.exception.InvalidOperationException;
import com.project_aegis.inventory_service.exception.InvalidStateTransitionException;
import com.project_aegis.inventory_service.exception.ResourceNotFoundException;
import com.project_aegis.inventory_service.mapper.CampaignSkuMapper;
import com.project_aegis.inventory_service.mapper.FlashCampaignMapper;
import com.project_aegis.inventory_service.repository.FlashCampaignRepository;
import com.project_aegis.inventory_service.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlashCampaignServiceTest {

    @Mock
    private FlashCampaignRepository flashCampaignRepository;

    @Mock
    private FlashCampaignMapper flashCampaignMapper;

    @Mock
    private CampaignSkuMapper campaignSkuMapper;

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private FlashCampaignService flashCampaignService;

    private UUID campaignId;
    private FlashCampaign campaign;
    private FlashCampaignRequest request;

    @BeforeEach
    void setUp() {
        campaignId = UUID.randomUUID();
        Instant now = Instant.now();

        campaign = FlashCampaign.builder()
                .id(campaignId)
                .name("Black Friday MacBook Drop")
                .startTime(now.plusSeconds(3600))
                .endTime(now.plusSeconds(7200))
                .status(FlashCampaignStatus.DRAFT)
                .flashCampaignSkus(new ArrayList<>())
                .build();

        request = FlashCampaignRequest.builder()
                .name("Black Friday MacBook Drop")
                .startTime(now.plusSeconds(3600))
                .endTime(now.plusSeconds(7200))
                .build();
    }

    @Test
    @DisplayName("Create campaign - Success")
    void createCampaign_Success() {
        when(flashCampaignRepository.existsByName(request.getName())).thenReturn(false);
        when(flashCampaignMapper.toEntity(request)).thenReturn(campaign);
        when(flashCampaignRepository.save(any(FlashCampaign.class))).thenReturn(campaign);
        when(flashCampaignMapper.toResponse(campaign)).thenReturn(FlashCampaignResponse.builder()
                .id(campaignId)
                .name(campaign.getName())
                .status("DRAFT")
                .build());

        ApiResponse<FlashCampaignResponse> response = flashCampaignService.addFlashCampaign(request);

        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getData().getId()).isEqualTo(campaignId);
        assertThat(response.getData().getStatus()).isEqualTo("DRAFT");
        verify(flashCampaignRepository).save(campaign);
    }

    @Test
    @DisplayName("Create campaign - Duplicate name throws InvalidOperationException")
    void createCampaign_DuplicateName_ThrowsException() {
        when(flashCampaignRepository.existsByName(request.getName())).thenReturn(true);

        assertThatThrownBy(() -> flashCampaignService.addFlashCampaign(request))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Create campaign - End time before start time throws InvalidOperationException")
    void createCampaign_InvalidTime_ThrowsException() {
        request.setEndTime(request.getStartTime().minusSeconds(100));

        assertThatThrownBy(() -> flashCampaignService.addFlashCampaign(request))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("End time must be after start time");
    }

    @Test
    @DisplayName("Publish campaign - Success")
    void publishCampaign_Success() {
        campaign.getFlashCampaignSkus().add(FlashCampaignSku.builder().id(UUID.randomUUID()).build());
        when(flashCampaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));
        when(flashCampaignRepository.save(any(FlashCampaign.class))).thenReturn(campaign);

        ApiResponse<CampaignStatusResponse> response = flashCampaignService.publishCampaign(campaignId);

        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getData().getStatus()).isEqualTo("PUBLISHED");
    }

    @Test
    @DisplayName("Publish campaign - No SKUs throws InvalidOperationException")
    void publishCampaign_NoSkus_ThrowsException() {
        when(flashCampaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));

        assertThatThrownBy(() -> flashCampaignService.publishCampaign(campaignId))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("without any SKUs");
    }

    @Test
    @DisplayName("Publish campaign - Invalid transition throws InvalidStateTransitionException")
    void publishCampaign_InvalidState_ThrowsException() {
        campaign.setStatus(FlashCampaignStatus.ACTIVE);
        when(flashCampaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));

        assertThatThrownBy(() -> flashCampaignService.publishCampaign(campaignId))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    @DisplayName("Activate campaign - Success")
    void activateCampaign_Success() {
        campaign.setStatus(FlashCampaignStatus.PUBLISHED);
        when(flashCampaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));
        when(flashCampaignRepository.save(any(FlashCampaign.class))).thenReturn(campaign);

        ApiResponse<CampaignStatusResponse> response = flashCampaignService.activateCampaign(campaignId);

        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getData().getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("End campaign - Success")
    void endCampaign_Success() {
        campaign.setStatus(FlashCampaignStatus.ACTIVE);
        when(flashCampaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));
        when(flashCampaignRepository.save(any(FlashCampaign.class))).thenReturn(campaign);

        ApiResponse<CampaignStatusResponse> response = flashCampaignService.endCampaign(campaignId);

        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getData().getStatus()).isEqualTo("ENDED");
    }

    @Test
    @DisplayName("Get campaign by ID - Not Found throws ResourceNotFoundException")
    void getCampaignById_NotFound_ThrowsException() {
        when(flashCampaignRepository.findById(campaignId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> flashCampaignService.getCampaignById(campaignId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
