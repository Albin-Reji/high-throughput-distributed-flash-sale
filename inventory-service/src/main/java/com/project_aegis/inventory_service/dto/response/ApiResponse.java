package com.project_aegis.inventory_service.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private Boolean success;
    private String message;
    private T data;

}
