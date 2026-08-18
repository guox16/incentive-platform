package com.incentive.points.domain;

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
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;

/**
 * 跨服务积分消费的预占记录。
 *
 * <p>业务号是 reserve、confirm 和 cancel 重试时共享的幂等键；本实体暂不修改账户余额，
 * 余额冻结与释放将在积分预占应用服务中接入。</p>
 */
@Entity
@Table(name = "point_reservations",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_point_reservation_business_id", columnNames = "business_id"),
    indexes = {
        @Index(name = "idx_point_reservation_user_created", columnList = "user_id,created_at"),
        @Index(name = "idx_point_reservation_status_expires", columnList = "status,expires_at")
    })
public class PointReservation {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "business_id", nullable = false, updatable = false)
  private Long businessId;

  @Column(name = "user_id", nullable = false, updatable = false)
  private Long userId;

  @Column(nullable = false, updatable = false)
  private long amount;

  @Column(nullable = false, length = 32, updatable = false)
  private String source;

  @Column(length = 200, updatable = false)
  private String remark;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private PointReservationStatus status;

  @Column(name = "confirmed_transaction_id")
  private Long confirmedTransactionId;

  @Column(name = "expires_at", nullable = false, updatable = false)
  private Instant expiresAt;

  @Column(name = "confirmed_at")
  private Instant confirmedAt;

  @Column(name = "cancelled_at")
  private Instant cancelledAt;

  @Column(name = "expired_at")
  private Instant expiredAt;

  @Version
  @Column(nullable = false)
  private long version;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected PointReservation() {}

  /** 创建一条尚未影响账户余额的积分预占记录。 */
  public PointReservation(Long businessId, Long userId, long amount, String source,
      String remark, Instant expiresAt, Instant now) {
    if (amount <= 0) throw new IllegalArgumentException("预占积分必须大于0");
    this.businessId = Objects.requireNonNull(businessId, "业务号不能为空");
    this.userId = Objects.requireNonNull(userId, "用户ID不能为空");
    this.source = requireText(source, "业务来源不能为空");
    this.amount = amount;
    this.remark = remark;
    this.expiresAt = Objects.requireNonNull(expiresAt, "过期时间不能为空");
    this.createdAt = Objects.requireNonNull(now, "创建时间不能为空");
    if (!expiresAt.isAfter(now)) throw new IllegalArgumentException("过期时间必须晚于创建时间");
    this.updatedAt = now;
    this.status = PointReservationStatus.RESERVED;
  }

  /** 将预占确认成正式扣减；重复确认同一流水时保持幂等。 */
  public void confirm(Long transactionId, Instant now) {
    Objects.requireNonNull(transactionId, "积分流水ID不能为空");
    Objects.requireNonNull(now, "确认时间不能为空");
    if (status == PointReservationStatus.CONFIRMED
        && transactionId.equals(confirmedTransactionId)) return;
    requireReserved("只有待确认的积分预占才能确认");
    status = PointReservationStatus.CONFIRMED;
    confirmedTransactionId = transactionId;
    confirmedAt = now;
    updatedAt = now;
  }

  /** 取消预占；重复取消时保持幂等。 */
  public void cancel(Instant now) {
    Objects.requireNonNull(now, "取消时间不能为空");
    if (status == PointReservationStatus.CANCELLED) return;
    requireReserved("只有待确认的积分预占才能取消");
    status = PointReservationStatus.CANCELLED;
    cancelledAt = now;
    updatedAt = now;
  }

  /** 将超过期限且仍未完成的预占标记为过期。 */
  public void expire(Instant now) {
    Objects.requireNonNull(now, "过期处理时间不能为空");
    requireReserved("只有待确认的积分预占才能过期");
    if (now.isBefore(expiresAt)) throw new IllegalStateException("积分预占尚未到期");
    status = PointReservationStatus.EXPIRED;
    expiredAt = now;
    updatedAt = now;
  }

  private void requireReserved(String message) {
    if (status != PointReservationStatus.RESERVED) throw new IllegalStateException(message);
  }

  private static String requireText(String value, String message) {
    if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
    return value.trim();
  }

  public Long getId() { return id; }
  public Long getBusinessId() { return businessId; }
  public Long getUserId() { return userId; }
  public long getAmount() { return amount; }
  public String getSource() { return source; }
  public String getRemark() { return remark; }
  public PointReservationStatus getStatus() { return status; }
  public Long getConfirmedTransactionId() { return confirmedTransactionId; }
  public Instant getExpiresAt() { return expiresAt; }
  public Instant getConfirmedAt() { return confirmedAt; }
  public Instant getCancelledAt() { return cancelledAt; }
  public Instant getExpiredAt() { return expiredAt; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
}
