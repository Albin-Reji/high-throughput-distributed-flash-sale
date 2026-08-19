package com.project_aegis.inventory_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(
        name = "flash_campaign_sku",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_campaign_sku",
                        columnNames = {"campaign_id", "sku_id"}
                )
        },
        indexes = {
                @Index(name = "idx_campaign_sku_campaign", columnList = "campaign_id"),
                @Index(name = "idx_campaign_sku_sku", columnList = "sku_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlashCampaignSku {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private FlashCampaign campaign;

    @Column(name = "sku_id", nullable = false)
    private UUID skuId;

    @NotNull
    @Positive
    @Column(name = "flash_price", nullable = false)
    private BigDecimal flashPrice;

    @NotNull
    @Positive
    @Column(name = "allocated_stock", nullable = false)
    private Integer allocatedStock;

    @NotNull
    @Positive
    @Builder.Default
    @Column(name = "max_per_user", nullable = false)
    private Integer maxPerUser = 10;
}