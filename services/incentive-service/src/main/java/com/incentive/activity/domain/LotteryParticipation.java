package com.incentive.activity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "lottery_records",
    uniqueConstraints = @UniqueConstraint(name = "uk_lottery_records_order",
        columnNames = "lottery_order_id"),
    indexes = @Index(name = "idx_lottery_records_status_updated",
        columnList = "status,updated_at"))
public class LotteryParticipation {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "lottery_order_id", nullable = false, updatable = false)
  private Long lotteryOrderId;

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
  @Column(name = "stock_no", updatable = false)
  private Long stockNo;
  @Column(name = "points_cost", nullable = false, updatable = false)
  private long pointsCost;
  @Column(name = "eligibility_result", columnDefinition = "json", updatable = false)
  private String eligibilityResult;
  @Column(name = "point_transaction_id")
  private Long pointTransactionId;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 24)
  private LotteryParticipationStatus status;
  @Column(name = "confirmed_at")
  private Instant confirmedAt;
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected LotteryParticipation() {}

  public LotteryParticipation(LotteryOrder order, Instant now) {
    this.lotteryOrderId = Objects.requireNonNull(order.getId(), "抽奖单ID不能为空");
    this.activityId = order.getActivityId();
    this.ruleId = order.getRuleId();
    this.ruleVersion = order.getRuleVersion();
    this.userId = order.getUserId();
    this.lotteryPrizeId = order.getLotteryPrizeId();
    this.prizeId = order.getPrizeId();
    this.prizeName = order.getPrizeName();
    this.prizeType = order.getPrizeType();
    this.coverUrl = order.getCoverUrl();
    this.awardPayload = order.getAwardPayload();
    this.stockNo = order.getStockNo();
    this.pointsCost = order.getPointsCost();
    this.eligibilityResult = order.getEligibilityResult();
    this.status = LotteryParticipationStatus.WAITING_CONFIRMATION;
    this.createdAt = Objects.requireNonNull(now, "创建时间不能为空");
    this.updatedAt = now;
  }

  public void markSuccess(Long transactionId, Instant now) {
    Objects.requireNonNull(transactionId, "积分流水ID不能为空");
    Objects.requireNonNull(now, "确认时间不能为空");
    if (status == LotteryParticipationStatus.SUCCESS) {
      if (!transactionId.equals(pointTransactionId)) {
        throw new IllegalStateException("重复完成抽奖记录时积分流水ID不一致");
      }
      return;
    }
    if (status != LotteryParticipationStatus.WAITING_CONFIRMATION) {
      throw new IllegalStateException("只有WAITING_CONFIRMATION抽奖记录可以完成");
    }
    pointTransactionId = transactionId;
    status = LotteryParticipationStatus.SUCCESS;
    confirmedAt = now;
    updatedAt = now;
  }

  public Long getId() { return id; }
  public Long getLotteryOrderId() { return lotteryOrderId; }
  public Long getUserId() { return userId; }
  public Long getPrizeId() { return prizeId; }
  public String getPrizeName() { return prizeName; }
  public PrizeType getPrizeType() { return prizeType; }
  public String getCoverUrl() { return coverUrl; }
  public String getAwardPayload() { return awardPayload; }
  public Long getStockNo() { return stockNo; }
  public long getPointsCost() { return pointsCost; }
  public Long getPointTransactionId() { return pointTransactionId; }
  public LotteryParticipationStatus getStatus() { return status; }
  public Instant getConfirmedAt() { return confirmedAt; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
}
