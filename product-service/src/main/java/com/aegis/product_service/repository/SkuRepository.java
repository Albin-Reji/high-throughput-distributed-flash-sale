package com.aegis.product_service.repository;

import com.aegis.product_service.entity.Sku;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SkuRepository extends JpaRepository<Sku, Long> {


    boolean existsBySkuCode(@NotBlank(message = "SKU code required") String skuCode);
}
