package com.project_aegis.order_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
        name = "order_shipping_addresses",
        indexes = {
                @Index(name = "idx_order_shipping_addresses_original_address_id", columnList = "original_address_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderShippingAddress {

    @Id
    @Column(name = "order_id")
    private UUID orderId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(name = "original_address_id")
    private UUID originalAddressId;

    @Column(name = "recipient_name", nullable = false)
    private String recipientName;

    @Column(name = "address_line_1", nullable = false)
    private String addressLine1;

    @Column(name = "address_line_2")
    private String addressLine2;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "state", nullable = false, length = 100)
    private String state;

    @Column(name = "postal_code", nullable = false, length = 20)
    private String postalCode;

    @Column(name = "country", nullable = false, length = 100)
    private String country;
}
