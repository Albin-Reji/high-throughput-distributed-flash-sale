package com.aegis.product_service.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode // Mandatory for composite keys
public class ProductAttributeId implements Serializable {
    private UUID product; // Must exactly match the field name in ProductAttribute
    private String attributeName;
}