package com.aegis.product_service.dto.common;

import lombok.Builder;

import java.util.UUID;


@Builder
public record SkuSuccessResponse(UUID id, String skuCode, String message) {
}
