package com.project_aegis.user_service.dto.customer.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerUpdateRequest {

    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;

}
