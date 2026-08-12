package com.project_aegis.user_service.controller.admin;

import com.project_aegis.user_service.dto.customer.response.CustomerProfileResponse;
import com.project_aegis.user_service.dto.request.StatusRequest;
import com.project_aegis.user_service.dto.response.ApiResponse;
import com.project_aegis.user_service.dto.response.PageResponse;
import com.project_aegis.user_service.dto.response.ProfileResponse;
import com.project_aegis.user_service.dto.response.StatusResponse;
import com.project_aegis.user_service.entity.CustomerProfile;
import com.project_aegis.user_service.service.CustomerProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/customers")
public class AdminCustomerProfileController {

    private final CustomerProfileService customerProfileService;

    @GetMapping
    public ResponseEntity<PageResponse<CustomerProfileResponse>> getAllCustomers(
            @PageableDefault(
                    page = 0,
                    size = 10,
                    direction = Sort.Direction.ASC
            )Pageable pageable
            ){
        return ResponseEntity.ok(customerProfileService.getAllCustomers( pageable));
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<ApiResponse<ProfileResponse>> getCustomerById(@PathVariable(name = "customerId")
                                                                        UUID customerId){
        return ResponseEntity.ok(customerProfileService.getCustomerByCustomerId(customerId));
    }

    @PatchMapping("/{customerId}/status")
    public ResponseEntity<ApiResponse<StatusResponse>> updateCustomerStatus(
            @PathVariable(name = "customerId") UUID customerId,
            @RequestBody StatusRequest request
            ){

        return ResponseEntity.ok(customerProfileService.updateCustomerStatus(customerId, request));
    }
    @DeleteMapping("/{customerId}")
    public ResponseEntity<ApiResponse<Void>> deleteCustomerByAdmin(
            @PathVariable(name = "customerId") UUID customerId
    ){

        return ResponseEntity.ok(customerProfileService.deleteCustomerByAdmin(customerId));
    }
}
