package com.project_aegis.inventory_service.repository;

import com.project_aegis.inventory_service.entity.FlashCampaignSku;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FlashCampaignSkuRepository extends JpaRepository<FlashCampaignSku, UUID> {

    List<FlashCampaignSku> findAllByCampaignId(UUID campaignId);

    Optional<FlashCampaignSku> findByCampaignIdAndSkuId(UUID campaignId, UUID skuId);

    boolean existsByCampaignIdAndSkuId(UUID campaignId, UUID skuId);
}
