package com.project_aegis.user_service.repository;

import com.project_aegis.user_service.entity.CustomerPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerPreferenceRepository extends JpaRepository<CustomerPreference, UUID> {
    Optional<CustomerPreference> findByCustomerId(UUID customerId);
}
