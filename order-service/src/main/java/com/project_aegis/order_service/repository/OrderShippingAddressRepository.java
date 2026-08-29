package com.project_aegis.order_service.repository;

import com.project_aegis.order_service.entity.OrderShippingAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OrderShippingAddressRepository extends JpaRepository<OrderShippingAddress, UUID> {
}
