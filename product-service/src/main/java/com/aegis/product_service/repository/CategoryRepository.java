package com.aegis.product_service.repository;

import com.aegis.product_service.entity.Category;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findAllByParentCategoryIsNull();

    boolean existsByName(@NotBlank(message = "Category name is required") String name);

    List<Category> findAllByNameContainingIgnoreCase(String name);

}
