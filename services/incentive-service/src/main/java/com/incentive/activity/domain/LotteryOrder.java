package com.incentive.activity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "lottery_orders",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_lottery_orders_request",
            columnNames = {"user_id", "activity_id", "request_id"}),
        @UniqueConstraint(name = "uk_lottery_orders_points_business",
            columnNames = "points_business_id")
    },
    indexes = {
        @Index(name = "idx_lottery_orders_user_created", columnList = "user_id,created_at"),
        @Index(name = "idx_lottery_orders_status_updated", columnList = "status,updated_at"),
        @Index(name = "idx_lottery_orders_retry",
            columnList = "status,next_retry_at,updated_at,id")
    })
public class LotteryOrder {
  @Id
  private Long id;

  @Column(name = "request_id", nullable = false, length = 64, updatable = false)
  private String requestId;
  @Column(name = "user_id", nullable = false, updatable = false)
  private Long userId;
  @Column(name = "activity_id", nullable = false, updatable = false)
  private Long activityId;
  @Column(name = "activity_code", nullable = false, length = 64, updatable = false)
  private String activityCode;
  @Column(name = "rule_id", nullable = false, updatable = false)
  private Long ruleId;
  @Column(name = "rule_version", nullable = false, updatable = false)
  private int ruleVersion;
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
  @Column(name = "points_business_id", nullable = false, updatable = false)
  private Long pointsBusinessId;
  @Column(name = "points_reservation_expires_at")
  private Instant pointsReservationExpiresAt;
  @Column(name = "points_balance_after")
  private Long pointsBalanceAfter;
  @Column(name = "eligibility_result", columnDefinition = "json", updatable = false)
  private String eligibilityResult;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 24)
  private LotteryOrderStatus status;
  @Column(name = "failure_code", length = 64)
  private String failureCode;
  @Column(name = "retry_count", nullable = false)
  private int retryCount;
  @Column(name = "next_retry_at")
  private Instant nextRetryAt;
  @Version
  @Column(nullable = false)
  private long version;
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected LotteryOrder() {}

  public LotteryOrder(Long id, String requestId, Long userId, IncentiveActivity activity,
      ParticipationRule rule, LotteryPrize prize, Long pointsBusinessId,
      String eligibilityResult, Instant now) {
    this.id = Objects.requireNonNull(id, "抽奖单ID不能为空");
    this.requestId = requireText(requestId, "requestId不能为空");
    this.userId = Objects.requireNonNull(userId, "用户ID不能为空");
    this.activityId = Objects.requireNonNull(activity.getId(), "活动ID不能为空");
    this.activityCode = requireText(activity.getCode(), "活动编码不能为空");
    this.ruleId = Objects.requireNonNull(rule.getId(), "规则ID不能为空");
    this.ruleVersion = rule.getRuleVersion();
    this.lotteryPrizeId = Objects.requireNonNull(prize.getId(), "活动奖品ID不能为空");
    this.prizeId = Objects.requireNonNull(prize.getPrizeId(), "奖品ID不能为空");
    this.prizeName = requireText(prize.getPrizeName(), "奖品名称不能为空");
    this.prizeType = Objects.requireNonNull(prize.getPrizeType(), "奖品类型不能为空");
    this.coverUrl = prize.getCoverUrl();
    this.awardPayload = prize.getAwardPayload();
    this.pointsCost = rule.getPointsCost();
    this.pointsBusinessId = Objects.requireNonNull(pointsBusinessId, "积分业务号不能为空");
    this.eligibilityResult = eligibilityResult;
    this.status = LotteryOrderStatus.INIT;
    this.retryCount = 0;
    this.createdAt = Objects.requireNonNull(now, "创建时间不能为空");
    this.updatedAt = now;
  }

  private static String requireText(String value, String message) {
    if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
    return value.trim();
  }

  public Long getId() { return id; }
  public String getRequestId() { return requestId; }
  public Long getUserId() { return userId; }
  public Long getActivityId() { return activityId; }
  public String getActivityCode() { return activityCode; }
  public Long getRuleId() { return ruleId; }
  public int getRuleVersion() { return ruleVersion; }
  public Long getLotteryPrizeId() { return lotteryPrizeId; }
  public Long getPrizeId() { return prizeId; }
  public String getPrizeName() { return prizeName; }
  public PrizeType getPrizeType() { return prizeType; }
  public String getCoverUrl() { return coverUrl; }
  public String getAwardPayload() { return awardPayload; }
  public long getPointsCost() { return pointsCost; }
  public Long getPointsBusinessId() { return pointsBusinessId; }
  public String getEligibilityResult() { return eligibilityResult; }
  public LotteryOrderStatus getStatus() { return status; }
  public String getFailureCode() { return failureCode; }
  public int getRetryCount() { return retryCount; }
  public Instant getNextRetryAt() { return nextRetryAt; }
  public Instant getPointsReservationExpiresAt() { return pointsReservationExpiresAt; }
  public Long getPointsBalanceAfter() { return pointsBalanceAfter; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }

  public void markPointsReserved(Instant expiresAt, long balanceAfter, Instant now) {
    Objects.requireNonNull(expiresAt, "积分预占过期时间不能为空");
    Objects.requireNonNull(now, "状态更新时间不能为空");
    if (balanceAfter < 0) throw new IllegalArgumentException("积分预占后余额不能为负数");
    if (status == LotteryOrderStatus.POINTS_RESERVED
        || status == LotteryOrderStatus.RESULT_SAVED
        || status == LotteryOrderStatus.SUCCESS) {
      if (!expiresAt.equals(pointsReservationExpiresAt)
          || !Long.valueOf(balanceAfter).equals(pointsBalanceAfter)) {
        throw new IllegalStateException("重复推进积分预占状态时返回快照不一致");
      }
      return;
    }
    if (status != LotteryOrderStatus.INIT) {
      throw new IllegalStateException("只有INIT抽奖单可以进入POINTS_RESERVED状态");
    }
    status = LotteryOrderStatus.POINTS_RESERVED;
    pointsReservationExpiresAt = expiresAt;
    pointsBalanceAfter = balanceAfter;
    clearFailure();
    updatedAt = now;
  }

  public void markResultSaved(Instant now) {
    Objects.requireNonNull(now, "状态更新时间不能为空");
    if (status == LotteryOrderStatus.RESULT_SAVED || status == LotteryOrderStatus.SUCCESS) return;
    if (status != LotteryOrderStatus.POINTS_RESERVED) {
      throw new IllegalStateException("只有POINTS_RESERVED抽奖单可以进入RESULT_SAVED状态");
    }
    status = LotteryOrderStatus.RESULT_SAVED;
    clearFailure();
    updatedAt = now;
  }

  public void markSuccess(Instant now) {
    Objects.requireNonNull(now, "状态更新时间不能为空");
    if (status == LotteryOrderStatus.SUCCESS) return;
    if (status != LotteryOrderStatus.RESULT_SAVED) {
      throw new IllegalStateException("只有RESULT_SAVED抽奖单可以进入SUCCESS状态");
    }
    status = LotteryOrderStatus.SUCCESS;
    clearFailure();
    updatedAt = now;
  }

  public void scheduleRetry(String code, Instant retryAt, Instant now) {
    String normalizedCode = requireText(code, "失败码不能为空");
    Objects.requireNonNull(retryAt, "下次重试时间不能为空");
    Objects.requireNonNull(now, "失败记录时间不能为空");
    if (status == LotteryOrderStatus.SUCCESS || status == LotteryOrderStatus.FAILED) {
      throw new IllegalStateException("终态抽奖单不能安排重试");
    }
    if (!retryAt.isAfter(now)) {
      throw new IllegalArgumentException("下次重试时间必须晚于当前时间");
    }
    failureCode = normalizedCode;
    retryCount = Math.addExact(retryCount, 1);
    nextRetryAt = retryAt;
    updatedAt = now;
  }

  public void markFailed(String code, Instant now) {
    failureCode = requireText(code, "失败码不能为空");
    Objects.requireNonNull(now, "失败记录时间不能为空");
    if (status == LotteryOrderStatus.SUCCESS) {
      throw new IllegalStateException("成功抽奖单不能标记失败");
    }
    if (status == LotteryOrderStatus.FAILED) return;
    retryCount = Math.addExact(retryCount, 1);
    nextRetryAt = null;
    status = LotteryOrderStatus.FAILED;
    updatedAt = now;
  }

  private void clearFailure() {
    failureCode = null;
    nextRetryAt = null;
  }
}
