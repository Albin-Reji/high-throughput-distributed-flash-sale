package com.project_aegis.inventory_service.repository;

import com.project_aegis.inventory_service.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    Optional<Inventory> findBySkuId(UUID skuId);

    boolean existsBySkuId(UUID skuId);
}
