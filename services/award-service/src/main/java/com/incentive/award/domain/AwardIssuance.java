package com.incentive.award.domain;

import com.incentive.award.messaging.AwardCommandMessage;
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
@Table(name = "award_issuances")
public class AwardIssuance {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(name = "command_key", nullable = false, unique = true, length = 64, updatable = false)
  private String commandKey;
  @Enumerated(EnumType.STRING)
  @Column(name = "source_type", nullable = false, length = 16, updatable = false)
  private AwardSourceType sourceType;
  @Column(name = "source_record_id", nullable = false, updatable = false)
  private Long sourceRecordId;
  @Column(name = "user_id", nullable = false, updatable = false)
  private Long userId;
  @Column(name = "award_id", nullable = false, updatable = false)
  private Long awardId;
  @Column(name = "award_name_snapshot", nullable = false, length = 100, updatable = false)
  private String awardName;
  @Enumerated(EnumType.STRING)
  @Column(name = "award_type_snapshot", nullable = false, length = 16, updatable = false)
  private AwardType awardType;
  @Column(name = "award_payload_snapshot", columnDefinition = "json", updatable = false)
  private String awardPayload;
  @Column(name = "stock_no", updatable = false)
  private Long stockNo;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private AwardIssuanceStatus status;
  @Column(name = "point_business_id", unique = true, updatable = false)
  private Long pointBusinessId;
  @Column(name = "result_ref", length = 128)
  private String resultRef;
  @Column(name = "retry_count", nullable = false)
  private int retryCount;
  @Column(name = "failure_code", length = 64)
  private String failureCode;
  @Column(name = "last_error", length = 500)
  private String lastError;
  @Column(name = "started_at", nullable = false, updatable = false)
  private Instant startedAt;
  @Column(name = "succeeded_at")
  private Instant succeededAt;
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected AwardIssuance() {}

  public AwardIssuance(AwardCommandMessage command, Long pointBusinessId, Instant now) {
    commandKey = command.commandKey();
    sourceType = command.sourceType();
    sourceRecordId = command.sourceRecordId();
    userId = command.userId();
    awardId = command.awardId();
    awardName = command.awardName();
    awardType = command.awardType();
    awardPayload = command.awardPayload();
    stockNo = command.stockNo();
    status = AwardIssuanceStatus.PROCESSING;
    this.pointBusinessId = pointBusinessId;
    retryCount = 0;
    startedAt = now;
    createdAt = now;
    updatedAt = now;
  }

  public void restart(Instant now) {
    if (status == AwardIssuanceStatus.SUCCEEDED) return;
    status = AwardIssuanceStatus.PROCESSING;
    failureCode = null;
    lastError = null;
    retryCount++;
    updatedAt = now;
  }

  public void succeed(String resultRef, Instant now) {
    if (resultRef == null || resultRef.isBlank()) {
      throw new IllegalArgumentException("发奖结果引用不能为空");
    }
    status = AwardIssuanceStatus.SUCCEEDED;
    this.resultRef = resultRef;
    failureCode = null;
    lastError = null;
    succeededAt = now;
    updatedAt = now;
  }

  public void fail(String code, String error, Instant now) {
    if (status == AwardIssuanceStatus.SUCCEEDED) return;
    status = AwardIssuanceStatus.FAILED;
    failureCode = code;
    String normalized = error == null || error.isBlank() ? "发奖失败" : error;
    lastError = normalized.substring(0, Math.min(normalized.length(), 500));
    updatedAt = now;
  }

  public Long getId() { return id; }
  public String getCommandKey() { return commandKey; }
  public AwardSourceType getSourceType() { return sourceType; }
  public Long getSourceRecordId() { return sourceRecordId; }
  public Long getUserId() { return userId; }
  public Long getAwardId() { return awardId; }
  public String getAwardName() { return awardName; }
  public AwardType getAwardType() { return awardType; }
  public String getAwardPayload() { return awardPayload; }
  public Long getStockNo() { return stockNo; }
  public AwardIssuanceStatus getStatus() { return status; }
  public Long getPointBusinessId() { return pointBusinessId; }
  public String getResultRef() { return resultRef; }
}
