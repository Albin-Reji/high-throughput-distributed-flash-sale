package com.project_aegis.user_service.repository;

import com.project_aegis.user_service.entity.CustomerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, UUID> {

    Optional<CustomerProfile> findByKeycloakUserId(String keycloakUserId);

    boolean existsByKeycloakUserId(String keycloakUserId);

    Optional<CustomerProfile> findByEmail(String email);
}
