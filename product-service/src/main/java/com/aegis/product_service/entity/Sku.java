package com.aegis.product_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "skus")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Sku {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String skuCode;

    private String color;

    private String size;

    @Column(nullable = false)
    private BigDecimal price; // Fixed: Now uses BigDecimal

    @JsonIgnore // Prevents infinite JSON recursion
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
}