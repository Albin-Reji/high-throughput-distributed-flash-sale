package com.aegis.product_service.repository;

import com.aegis.product_service.entity.Sku;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SkuRepository extends JpaRepository<Sku, Long> {


    boolean existsBySkuCode(@NotBlank(message = "SKU code required") String skuCode);

    Page<Sku> findByProductId(Pageable pageable, UUID productId);
}
