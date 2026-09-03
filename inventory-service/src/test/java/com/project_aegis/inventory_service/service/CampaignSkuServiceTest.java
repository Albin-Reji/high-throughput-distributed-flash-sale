package com.project_aegis.inventory_service.service;

import com.project_aegis.inventory_service.client.ProductClient;
import com.project_aegis.inventory_service.dto.campaign.request.CampaignSkuRequest;
import com.project_aegis.inventory_service.dto.campaign.request.CampaignSkuUpdateRequest;
import com.project_aegis.inventory_service.dto.campaign.response.CampaignSkuResponse;
import com.project_aegis.inventory_service.dto.response.ApiResponse;
import com.project_aegis.inventory_service.dto.response.SkuResponse;
import com.project_aegis.inventory_service.entity.FlashCampaign;
import com.project_aegis.inventory_service.entity.FlashCampaignSku;
import com.project_aegis.inventory_service.entity.FlashCampaignStatus;
import com.project_aegis.inventory_service.exception.InvalidOperationException;
import com.project_aegis.inventory_service.exception.ResourceNotFoundException;
import com.project_aegis.inventory_service.mapper.CampaignSkuMapper;
import com.project_aegis.inventory_service.repository.FlashCampaignSkuRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CampaignSkuService Unit Tests")
class CampaignSkuServiceTest {

    @Mock
    private FlashCampaignService flashCampaignService;

    @Mock
    private FlashCampaignSkuRepository flashCampaignSkuRepository;

    @Mock
    private CampaignSkuMapper campaignSkuMapper;

    @Mock
    private ProductClient productClient;

    @InjectMocks
    private CampaignSkuService campaignSkuService;

    private UUID campaignId;
    private UUID campaignSkuId;
    private UUID skuId;
    private FlashCampaign draftCampaign;
    private FlashCampaignSku campaignSku;
    private CampaignSkuResponse campaignSkuResponse;

    @BeforeEach
    void setUp() {
        campaignId = UUID.randomUUID();
        campaignSkuId = UUID.randomUUID();
        skuId = UUID.randomUUID();

        draftCampaign = FlashCampaign.builder()
                .id(campaignId)
                .name("Test Campaign")
                .status(FlashCampaignStatus.DRAFT)
                .build();

        campaignSku = FlashCampaignSku.builder()
                .id(campaignSkuId)
                .campaign(draftCampaign)
                .skuId(skuId)
                .flashPrice(BigDecimal.valueOf(99.99))
                .allocatedStock(100)
                .maxPerUser(2)
                .build();

        campaignSkuResponse = CampaignSkuResponse.builder()
                .id(campaignSkuId)
                .skuId(skuId)
                .flashPrice(BigDecimal.valueOf(99.99))
                .allocatedStock(100)
                .maxPerUser(2)
                .build();
    }

    // ──────────────────────────────────────────────────────────────
    //  addSkuToCampaign
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("addSkuToCampaign")
    class AddSkuToCampaign {

        private CampaignSkuRequest request;

        @BeforeEach
        void setUp() {
            request = CampaignSkuRequest.builder()
                    .skuId(skuId)
                    .flashPrice(BigDecimal.valueOf(99.99))
                    .allocatedStock(100)
                    .maxPerUser(2)
                    .build();
        }

        @Test
        @DisplayName("should add SKU to campaign successfully")
        void shouldAddSkuSuccessfully() {
            when(flashCampaignService.findCampaignOrThrow(campaignId)).thenReturn(draftCampaign);
            when(flashCampaignSkuRepository.existsByCampaignIdAndSkuId(campaignId, skuId)).thenReturn(false);
            when(productClient.getSku(skuId)).thenReturn(new SkuResponse());
            when(campaignSkuMapper.toEntity(request, draftCampaign)).thenReturn(campaignSku);
            when(campaignSkuMapper.toResponse(campaignSku)).thenReturn(campaignSkuResponse);

            ApiResponse<CampaignSkuResponse> response = campaignSkuService.addSkuToCampaign(campaignId, request);

            assertThat(response.getSuccess()).isTrue();
            assertThat(response.getMessage()).contains("SKU added");
            assertThat(response.getData()
                    .getSkuId()).isEqualTo(skuId);
            verify(flashCampaignSkuRepository).save(campaignSku);
        }

        @Test
        @DisplayName("should throw InvalidOperationException when SKU already added to campaign")
        void shouldThrowWhenDuplicateSku() {
            when(flashCampaignService.findCampaignOrThrow(campaignId)).thenReturn(draftCampaign);
            when(flashCampaignSkuRepository.existsByCampaignIdAndSkuId(campaignId, skuId)).thenReturn(true);

            assertThatThrownBy(() -> campaignSkuService.addSkuToCampaign(campaignId, request))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessageContaining("already added");

            verify(flashCampaignSkuRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw InvalidOperationException when campaign is not in DRAFT status")
        void shouldThrowWhenCampaignNotDraft() {
            draftCampaign.setStatus(FlashCampaignStatus.ACTIVE);
            when(flashCampaignService.findCampaignOrThrow(campaignId)).thenReturn(draftCampaign);

            assertThatThrownBy(() -> campaignSkuService.addSkuToCampaign(campaignId, request))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessageContaining("DRAFT");

            verify(flashCampaignSkuRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when SKU does not exist in product service")
        void shouldThrowWhenSkuInvalid() {
            when(flashCampaignService.findCampaignOrThrow(campaignId)).thenReturn(draftCampaign);
            when(flashCampaignSkuRepository.existsByCampaignIdAndSkuId(campaignId, skuId)).thenReturn(false);
            when(productClient.getSku(skuId)).thenThrow(new RuntimeException("Service unavailable"));

            assertThatThrownBy(() -> campaignSkuService.addSkuToCampaign(campaignId, request))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(flashCampaignSkuRepository, never()).save(any());
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  updateCampaignSku
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateCampaignSku")
    class UpdateCampaignSku {

        @Test
        @DisplayName("should update campaign SKU fields successfully")
        void shouldUpdateSuccessfully() {
            CampaignSkuUpdateRequest request = CampaignSkuUpdateRequest.builder()
                    .flashPrice(BigDecimal.valueOf(79.99))
                    .allocatedStock(200)
                    .maxPerUser(5)
                    .build();

            when(flashCampaignService.findCampaignOrThrow(campaignId)).thenReturn(draftCampaign);
            when(flashCampaignSkuRepository.findById(campaignSkuId)).thenReturn(Optional.of(campaignSku));
            when(campaignSkuMapper.toResponse(campaignSku)).thenReturn(campaignSkuResponse);

            ApiResponse<CampaignSkuResponse> response =
                    campaignSkuService.updateCampaignSku(campaignId, campaignSkuId, request);

            assertThat(response.getSuccess()).isTrue();
            assertThat(campaignSku.getFlashPrice()).isEqualTo(BigDecimal.valueOf(79.99));
            assertThat(campaignSku.getAllocatedStock()).isEqualTo(200);
            assertThat(campaignSku.getMaxPerUser()).isEqualTo(5);
            verify(flashCampaignSkuRepository).save(campaignSku);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when campaignSku not found")
        void shouldThrowWhenCampaignSkuNotFound() {
            CampaignSkuUpdateRequest request = CampaignSkuUpdateRequest.builder()
                    .build();

            when(flashCampaignService.findCampaignOrThrow(campaignId)).thenReturn(draftCampaign);
            when(flashCampaignSkuRepository.findById(campaignSkuId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> campaignSkuService.updateCampaignSku(campaignId, campaignSkuId, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when campaignSku belongs to different campaign")
        void shouldThrowWhenWrongCampaign() {
            UUID otherCampaignId = UUID.randomUUID();
            FlashCampaign otherCampaign = FlashCampaign.builder()
                    .id(otherCampaignId)
                    .status(FlashCampaignStatus.DRAFT)
                    .build();
            campaignSku.setCampaign(otherCampaign);

            CampaignSkuUpdateRequest request = CampaignSkuUpdateRequest.builder()
                    .build();

            when(flashCampaignService.findCampaignOrThrow(campaignId)).thenReturn(draftCampaign);
            when(flashCampaignSkuRepository.findById(campaignSkuId)).thenReturn(Optional.of(campaignSku));

            assertThatThrownBy(() -> campaignSkuService.updateCampaignSku(campaignId, campaignSkuId, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should throw InvalidOperationException when campaign is not in DRAFT status")
        void shouldThrowWhenNotDraft() {
            draftCampaign.setStatus(FlashCampaignStatus.PUBLISHED);
            CampaignSkuUpdateRequest request = CampaignSkuUpdateRequest.builder()
                    .build();

            when(flashCampaignService.findCampaignOrThrow(campaignId)).thenReturn(draftCampaign);

            assertThatThrownBy(() -> campaignSkuService.updateCampaignSku(campaignId, campaignSkuId, request))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessageContaining("DRAFT");
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  removeSkuFromCampaign
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("removeSkuFromCampaign")
    class RemoveSkuFromCampaign {

        @Test
        @DisplayName("should remove SKU from DRAFT campaign successfully")
        void shouldRemoveSuccessfully() {
            when(flashCampaignService.findCampaignOrThrow(campaignId)).thenReturn(draftCampaign);
            when(flashCampaignSkuRepository.findById(campaignSkuId)).thenReturn(Optional.of(campaignSku));

            ApiResponse<Void> response = campaignSkuService.removeSkuFromCampaign(campaignId, campaignSkuId);

            assertThat(response.getSuccess()).isTrue();
            assertThat(response.getMessage()).contains("removed");
            verify(flashCampaignSkuRepository).delete(campaignSku);
        }

        @Test
        @DisplayName("should throw InvalidOperationException when campaign is ACTIVE")
        void shouldThrowWhenActive() {
            draftCampaign.setStatus(FlashCampaignStatus.ACTIVE);
            when(flashCampaignService.findCampaignOrThrow(campaignId)).thenReturn(draftCampaign);

            assertThatThrownBy(() -> campaignSkuService.removeSkuFromCampaign(campaignId, campaignSkuId))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessageContaining("ACTIVE");

            verify(flashCampaignSkuRepository, never()).delete(any());
        }

        @Test
        @DisplayName("should throw InvalidOperationException when campaign is ENDED")
        void shouldThrowWhenEnded() {
            draftCampaign.setStatus(FlashCampaignStatus.ENDED);
            when(flashCampaignService.findCampaignOrThrow(campaignId)).thenReturn(draftCampaign);

            assertThatThrownBy(() -> campaignSkuService.removeSkuFromCampaign(campaignId, campaignSkuId))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessageContaining("ENDED");

            verify(flashCampaignSkuRepository, never()).delete(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when campaignSku belongs to different campaign")
        void shouldThrowWhenWrongCampaign() {
            UUID otherCampaignId = UUID.randomUUID();
            FlashCampaign otherCampaign = FlashCampaign.builder()
                    .id(otherCampaignId)
                    .status(FlashCampaignStatus.DRAFT)
                    .build();
            campaignSku.setCampaign(otherCampaign);

            when(flashCampaignService.findCampaignOrThrow(campaignId)).thenReturn(draftCampaign);
            when(flashCampaignSkuRepository.findById(campaignSkuId)).thenReturn(Optional.of(campaignSku));

            assertThatThrownBy(() -> campaignSkuService.removeSkuFromCampaign(campaignId, campaignSkuId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
