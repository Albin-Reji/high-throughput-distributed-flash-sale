package com.aegis.product_service.repository;

import com.aegis.product_service.entity.ProductAttribute;
import com.aegis.product_service.entity.ProductAttributeId;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductAttributeRepository extends JpaRepository<ProductAttribute, ProductAttributeId> {
    Page<ProductAttribute> findByProductId(Pageable pageable, UUID productId);

    Optional<ProductAttribute> findByProductIdAndAttributeName(UUID productId, @NotBlank(message = "Attribute name required") String name);
}
