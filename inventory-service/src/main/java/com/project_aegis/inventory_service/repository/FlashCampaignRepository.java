package com.project_aegis.inventory_service.repository;

import com.project_aegis.inventory_service.entity.FlashCampaign;
import com.project_aegis.inventory_service.entity.FlashCampaignStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FlashCampaignRepository extends JpaRepository<FlashCampaign, UUID> {

    Page<FlashCampaign> findAllByStatus(Pageable pageable, FlashCampaignStatus status);

    Page<FlashCampaign> findAllByStatusIn(List<FlashCampaignStatus> statuses, Pageable pageable);

    boolean existsByName(String name);
}

