package com.project_aegis.user_service.controller.publicapi;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/public/customers")

public class CustomerProfileController {

    @PostMapping
    public ResponseEntity<String> getCustomerProfile(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok("Customer profile retrieved successfully : " + jwt.getSubject());
    }
}
