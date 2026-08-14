package com.incentive.activity.domain;

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
import java.time.Instant;

@Entity
@Table(name = "incentive_activities")
public class IncentiveActivity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 64)
  private String code;

  @Enumerated(EnumType.STRING)
  @Column(name = "activity_type", nullable = false, length = 32)
  private ActivityType type;

  @Column(nullable = false, length = 100)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private ActivityStatus status;

  @Column(name = "starts_at", nullable = false)
  private Instant startsAt;

  @Column(name = "ends_at")
  private Instant endsAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected IncentiveActivity() {}

  public IncentiveActivity(String code, ActivityType type, String name,
      Instant startsAt, Instant endsAt) {
    this.code = code;
    this.type = type;
    this.name = name;
    this.status = ActivityStatus.DRAFT;
    this.startsAt = startsAt;
    this.endsAt = endsAt;
  }

  @PrePersist
  void beforeInsert() {
    createdAt = Instant.now();
    updatedAt = createdAt;
  }

  @PreUpdate
  void beforeUpdate() {
    updatedAt = Instant.now();
  }

  public void update(String name, ActivityStatus status, Instant startsAt, Instant endsAt) {
    this.name = name;
    this.status = status;
    this.startsAt = startsAt;
    this.endsAt = endsAt;
  }

  public Long getId() { return id; }
  public String getCode() { return code; }
  public ActivityType getType() { return type; }
  public String getName() { return name; }
  public ActivityStatus getStatus() { return status; }
  public Instant getStartsAt() { return startsAt; }
  public Instant getEndsAt() { return endsAt; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
}
