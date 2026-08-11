package com.incentive.activity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "daily_check_ins",
    uniqueConstraints = @UniqueConstraint(name = "uk_daily_check_ins_user_date", columnNames = {"user_id", "check_in_date"}))
public class DailyCheckIn {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false, updatable = false)
  private Long userId;

  @Column(name = "check_in_date", nullable = false, updatable = false)
  private LocalDate checkInDate;

  @Column(name = "streak_days", nullable = false, updatable = false)
  private int streakDays;

  @Column(name = "reward_points", nullable = false, updatable = false)
  private long rewardPoints;

  @Column(name = "reward_rule_version", nullable = false, updatable = false)
  private int rewardRuleVersion;

  @Enumerated(EnumType.STRING)
  @Column(name = "reward_status", nullable = false, length = 16)
  private RewardStatus rewardStatus;

  @Column(name = "point_transaction_id")
  private Long pointTransactionId;

  @Column(name = "rewarded_at")
  private Instant rewardedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected DailyCheckIn() {}

  public DailyCheckIn(Long userId, LocalDate checkInDate, int streakDays, long rewardPoints, Instant now) {
    this.userId = userId;
    this.checkInDate = checkInDate;
    this.streakDays = streakDays;
    this.rewardPoints = rewardPoints;
    this.rewardRuleVersion = 1;
    this.rewardStatus = RewardStatus.PENDING;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public void markAwarded(Long transactionId, Instant now) {
    this.pointTransactionId = transactionId;
    this.rewardStatus = RewardStatus.AWARDED;
    this.rewardedAt = now;
    this.updatedAt = now;
  }

  public Long getId() { return id; }
  public Long getUserId() { return userId; }
  public LocalDate getCheckInDate() { return checkInDate; }
  public int getStreakDays() { return streakDays; }
  public long getRewardPoints() { return rewardPoints; }
  public RewardStatus getRewardStatus() { return rewardStatus; }
  public Long getPointTransactionId() { return pointTransactionId; }
}
