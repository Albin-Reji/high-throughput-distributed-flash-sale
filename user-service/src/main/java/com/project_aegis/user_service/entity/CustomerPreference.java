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
@Table(name = "customer_preferences")
public class CustomerPreference {

    @Id
    @OneToOne(fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "customer_id",
            nullable = false
    )
    private CustomerProfile customer;

    @Column(
            name = "marketing_emails",
            nullable = false
    )
    @Builder.Default
    private boolean marketingEmailsEnabled = false;

    @Column(
            name = "sms_notifications",
            nullable = false
    )
    @Builder.Default
    private boolean smsNotificationsEnabled = false;

    @Column(
            name = "preferred_currency",
            nullable = false,
            length = 3
    )
    @Builder.Default
    private String preferredCurrency = "INR";
}