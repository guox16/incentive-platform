package com.incentive.activity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "lottery_participations")
public class LotteryParticipation {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "activity_id", nullable = false, updatable = false)
  private Long activityId;
  @Column(name = "rule_id", nullable = false, updatable = false)
  private Long ruleId;
  @Column(name = "rule_version", nullable = false, updatable = false)
  private int ruleVersion;
  @Column(name = "user_id", nullable = false, updatable = false)
  private Long userId;
  @Column(name = "lottery_prize_id", nullable = false, updatable = false)
  private Long lotteryPrizeId;
  @Column(name = "prize_id", nullable = false, updatable = false)
  private Long prizeId;
  @Column(name = "prize_name_snapshot", nullable = false, length = 100, updatable = false)
  private String prizeName;
  @Enumerated(EnumType.STRING)
  @Column(name = "prize_type_snapshot", nullable = false, length = 16, updatable = false)
  private PrizeType prizeType;
  @Column(name = "cover_url_snapshot", length = 500, updatable = false)
  private String coverUrl;
  @Column(name = "award_payload_snapshot", columnDefinition = "json", updatable = false)
  private String awardPayload;
  @Column(name = "points_cost", nullable = false, updatable = false)
  private long pointsCost;
  @Column(name = "eligibility_result", columnDefinition = "json", updatable = false)
  private String eligibilityResult;
  @Column(name = "point_transaction_id", nullable = false, updatable = false)
  private Long pointTransactionId;
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected LotteryParticipation() {}

  public LotteryParticipation(IncentiveActivity activity, ParticipationRule rule, Long userId,
      LotteryPrize prize, String eligibilityResult, Long pointTransactionId, Instant now) {
    this.activityId = activity.getId();
    this.ruleId = rule.getId();
    this.ruleVersion = rule.getRuleVersion();
    this.userId = userId;
    this.lotteryPrizeId = prize.getId();
    this.prizeId = prize.getPrizeId();
    this.prizeName = prize.getPrizeName();
    this.prizeType = prize.getPrizeType();
    this.coverUrl = prize.getCoverUrl();
    this.awardPayload = prize.getAwardPayload();
    this.pointsCost = rule.getPointsCost();
    this.eligibilityResult = eligibilityResult;
    this.pointTransactionId = pointTransactionId;
    this.createdAt = now;
  }

  public Long getId() { return id; }
  public Long getUserId() { return userId; }
  public Long getPrizeId() { return prizeId; }
  public String getPrizeName() { return prizeName; }
  public PrizeType getPrizeType() { return prizeType; }
  public String getCoverUrl() { return coverUrl; }
  public String getAwardPayload() { return awardPayload; }
  public long getPointsCost() { return pointsCost; }
  public Long getPointTransactionId() { return pointTransactionId; }
  public Instant getCreatedAt() { return createdAt; }
}
