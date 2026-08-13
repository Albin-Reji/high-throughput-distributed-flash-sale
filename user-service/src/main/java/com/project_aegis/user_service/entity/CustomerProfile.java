package com.project_aegis.user_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "customer_profiles",
        indexes = {
                @Index(
                        name = "idx_customer_profiles_email",
                        columnList = "email"
                ),
                @Index(
                        name = "idx_customer_profiles_keycloak_user_id",
                        columnList = "keycloak_user_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(
            name = "keycloak_user_id",
            nullable = false,
            unique = true,
            updatable = false,
            length = 36
    )
    private String keycloakUserId;

    @Column(
            nullable = false,
            unique = true
    )
    @Email
    private String email;

    @Column(
            nullable = false,
            length = 100
    )
    private String firstName;

    @Column(
            nullable = false,
            length = 100
    )
    private String lastName;

    @Column(length = 16)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus accountStatus;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "customer",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL)
    @Builder.Default
    private List<CustomerAddress> addresses=new ArrayList<>();

    @OneToOne(mappedBy = "customer",
            cascade = CascadeType.ALL
            )
    private CustomerPreference preference;
}