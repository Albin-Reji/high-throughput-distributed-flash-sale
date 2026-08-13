package com.project_aegis.user_service.repository;

import com.project_aegis.user_service.entity.CustomerAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, UUID> {
    List<CustomerAddress> findAllByCustomerId(UUID id);

    Optional<CustomerAddress> findByIdAndCustomerId(UUID addressId, UUID customerId);

    @Modifying
    @Query("UPDATE CustomerAddress a SET a.defaultAddress = false WHERE a.customer.id = :customerId AND a.defaultAddress = true")
    void clearDefaultAddresses(@Param("customerId") UUID customerId);
}
