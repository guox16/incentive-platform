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
@Table(name = "activity_participation_rules")
public class ParticipationRule {
  public enum Status { DRAFT, ACTIVE, RETIRED }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "activity_id", nullable = false, updatable = false)
  private Long activityId;

  @Column(name = "rule_version", nullable = false, updatable = false)
  private int ruleVersion;

  @Column(name = "points_cost", nullable = false, updatable = false)
  private long pointsCost;

  @Column(name = "daily_limit", updatable = false)
  private Integer dailyLimit;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private Status status;

  @Column(name = "effective_from", nullable = false, updatable = false)
  private Instant effectiveFrom;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected ParticipationRule() {}

  public ParticipationRule(Long activityId, int ruleVersion, long pointsCost,
      Integer dailyLimit, Instant effectiveFrom) {
    this.activityId = activityId;
    this.ruleVersion = ruleVersion;
    this.pointsCost = pointsCost;
    this.dailyLimit = dailyLimit;
    this.status = Status.ACTIVE;
    this.effectiveFrom = effectiveFrom;
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

  public Long getId() { return id; }
  public Long getActivityId() { return activityId; }
  public int getRuleVersion() { return ruleVersion; }
  public long getPointsCost() { return pointsCost; }
  public Integer getDailyLimit() { return dailyLimit; }
  public Status getStatus() { return status; }
  public Instant getEffectiveFrom() { return effectiveFrom; }
}
