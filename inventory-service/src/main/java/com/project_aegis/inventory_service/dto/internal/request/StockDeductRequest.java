package com.project_aegis.inventory_service.dto.internal.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockDeductRequest {

    @NotNull
    private UUID orderId;
}
