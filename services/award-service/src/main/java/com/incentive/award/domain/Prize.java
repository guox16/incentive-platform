package com.incentive.award.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "prizes")
public class Prize {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @Column(nullable = false, unique = true, updatable = false, length = 64) private String code;
  @Column(nullable = false, length = 100) private String name;
  @Enumerated(EnumType.STRING) @Column(name = "prize_type", nullable = false, length = 16) private PrizeType type;
  @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private PrizeStatus status;
  @Column(name = "available_stock", nullable = false) private long availableStock;
  @Column(name = "award_payload", columnDefinition = "json") private String awardPayload;
  @Version @Column(nullable = false) private long version;
  @Column(name = "deleted_at") private Instant deletedAt;
  @Column(nullable = false, updatable = false) private Instant createdAt;
  @Column(nullable = false) private Instant updatedAt;

  protected Prize() {}
  public Prize(String code, String name, PrizeType type, long availableStock, String awardPayload) {
    this.code = code; this.name = name; this.type = type; this.availableStock = availableStock;
    this.awardPayload = awardPayload; this.status = PrizeStatus.DRAFT;
  }
  @PrePersist void beforeInsert() { createdAt = Instant.now(); updatedAt = createdAt; }
  @PreUpdate void beforeUpdate() { updatedAt = Instant.now(); }
  public void update(String name, PrizeType type, PrizeStatus status, String awardPayload) {
    this.name = name; this.type = type; this.status = status; this.awardPayload = awardPayload;
  }
  public void adjustStock(long changeAmount) {
    long next = Math.addExact(availableStock, changeAmount);
    if (next < 0) throw new IllegalArgumentException("库存不能小于零");
    availableStock = next;
  }
  public void delete() { status = PrizeStatus.DELETED; deletedAt = Instant.now(); }
  public Long getId() { return id; } public String getCode() { return code; } public String getName() { return name; }
  public PrizeType getType() { return type; } public PrizeStatus getStatus() { return status; }
  public long getAvailableStock() { return availableStock; } public String getAwardPayload() { return awardPayload; }
  public Instant getDeletedAt() { return deletedAt; } public Instant getCreatedAt() { return createdAt; } public Instant getUpdatedAt() { return updatedAt; }
}
