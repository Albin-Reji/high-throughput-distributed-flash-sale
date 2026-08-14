package com.aegis.product_service.dto.common;

import java.util.UUID;


public record ProductSuccessResponse(UUID id, String title, String message) {
}
