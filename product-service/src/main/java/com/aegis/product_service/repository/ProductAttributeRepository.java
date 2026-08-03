package com.aegis.product_service.repository;

import com.aegis.product_service.entity.ProductAttribute;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProductAttributeRepository extends JpaRepository<ProductAttribute,Long> {
    Page<ProductAttribute> findByProductId(Pageable pageable, UUID productId);
}
