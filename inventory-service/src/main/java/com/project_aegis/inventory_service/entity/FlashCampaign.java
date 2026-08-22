package com.project_aegis.inventory_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "flash_campaign")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlashCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FlashCampaignStatus status;

    @Builder.Default
    @OneToMany(
            mappedBy = "campaign",
            fetch = FetchType.LAZY
    )
    private List<FlashCampaignSku> flashCampaignSkus = new ArrayList<>();
}