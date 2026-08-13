package com.project_aegis.user_service.entity;


import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(
        name = "customer_addresses",
        indexes = {
                @Index(
                        name = "idx_customer_addresses_customer_id",
                        columnList = "customer_id"
                ),
                @Index(
                        name = "idx_customer_addresses_id",
                        columnList = "id"
                )
        }
)
public class CustomerAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "customer_id",
            nullable = false
    )
    private CustomerProfile customer;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "address_type",
            nullable = false,
            length = 20
    )
    private AddressType addressType;

    @Column(
            name = "address_line_1",
            nullable = false
    )
    private String addressLine1;

    @Column(
            name = "address_line_2"
    )
    private String addressLine2;

    @Column(
            nullable = false,
            length = 100
    )
    private String city;

    @Column(
            nullable = false,
            length = 100
    )
    private String state;

    @Column(
            name = "postal_code",
            nullable = false,
            length = 20
    )
    private String postalCode;

    @Column(
            nullable = false,
            length = 100
    )
    private String country;

    @Column(
            name = "is_default",
            nullable = false
    )
    @Builder.Default
    private boolean defaultAddress = false;
}