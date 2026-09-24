package com.project_aegis.inventory_service.repository;

import com.project_aegis.inventory_service.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    Optional<Inventory> findBySkuId(UUID skuId);

    boolean existsBySkuId(UUID skuId);

    /**  Atomically reserves stock. *
     *  The update succeeds only when enough stock is available. *
     *  @return 1 if stock was successfully reserved, 0 otherwise */
    @Modifying
    @Query(""" 
        UPDATE Inventory i
        SET i.availableQuantity = i.availableQuantity - :quantity
        WHERE i.skuId = :skuId
        AND i.availableQuantity >= :quantity""")
    int reserveStock(
            @Param("skuId") UUID skuId,
            @Param("quantity") int quantity
    );
}
