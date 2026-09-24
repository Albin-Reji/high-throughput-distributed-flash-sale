package com.aegis.product_service.repository;

import com.aegis.product_service.entity.Sku;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SkuRepository extends JpaRepository<Sku, UUID> {


    boolean existsBySkuCode(@NotBlank(message = "SKU code required") String skuCode);

    Page<Sku> findByProductId(Pageable pageable, UUID productId);

    Optional<Sku> findByIdAndProductId(UUID id, UUID productId);

    @Query("SELECT s FROM Sku s LEFT JOIN FETCH s.product WHERE s.id IN :skuIds")
    List<Sku> findAllByIdInWithProduct(@Param("skuIds") Collection<UUID> skuIds);

}
