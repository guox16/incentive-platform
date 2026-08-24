package com.incentive.award.domain;

import com.incentive.award.dto.AwardUpsertRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "awards")
public class Award {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 64)
  private String code;

  @Column(nullable = false, length = 100)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(name = "award_type", nullable = false, length = 16)
  private AwardType type;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private AwardStatus status;

  @Column(name = "cover_url", length = 500)
  private String coverUrl;

  @Column(name = "award_payload", columnDefinition = "json")
  private String awardPayload;

  @Column(name = "total_stock", nullable = false)
  private long totalStock;

  @Column(name = "available_stock", nullable = false)
  private long availableStock;

  @Version
  @Column(nullable = false)
  private long version;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Award() {}

  public Award(String code, AwardUpsertRequest request, Instant now) {
    this.code = code;
    this.createdAt = now;
    apply(request, now);
  }

  public void update(AwardUpsertRequest request, Instant now) {
    apply(request, now);
  }

  public void softDelete(Instant now) {
    status = AwardStatus.DELETED;
    updatedAt = now;
  }

  public void adjustInventory(long changeAmount, Instant now) {
    long nextTotal = Math.addExact(totalStock, changeAmount);
    long nextAvailable = Math.addExact(availableStock, changeAmount);
    if (nextTotal < 0 || nextAvailable < 0) {
      throw new IllegalArgumentException("库存不能小于零");
    }
    totalStock = nextTotal;
    availableStock = nextAvailable;
    updatedAt = now;
  }

  public void consumeInventory(Instant now) {
    if (availableStock <= 0) {
      throw new IllegalArgumentException("可用库存不足");
    }
    availableStock--;
    updatedAt = now;
  }

  private void apply(AwardUpsertRequest request, Instant now) {
    name = request.name().trim();
    type = request.type();
    status = request.status();
    coverUrl = normalize(request.coverUrl());
    awardPayload = normalize(request.awardPayload());
    totalStock = request.totalStock();
    availableStock = request.availableStock();
    updatedAt = now;
  }

  private String normalize(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  public Long getId() { return id; }
  public String getCode() { return code; }
  public String getName() { return name; }
  public AwardType getType() { return type; }
  public AwardStatus getStatus() { return status; }
  public String getCoverUrl() { return coverUrl; }
  public String getAwardPayload() { return awardPayload; }
  public long getTotalStock() { return totalStock; }
  public long getAvailableStock() { return availableStock; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
}
