package com.incentive.award.domain;

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
@Table(name = "user_awards")
public class UserAward {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(name = "user_id", nullable = false, updatable = false)
  private Long userId;
  @Column(name = "award_id", nullable = false, updatable = false)
  private Long awardId;
  @Column(name = "issuance_id", nullable = false, unique = true, updatable = false)
  private Long issuanceId;
  @Enumerated(EnumType.STRING)
  @Column(name = "source_type", nullable = false, length = 16, updatable = false)
  private AwardSourceType sourceType;
  @Column(name = "source_record_id", nullable = false, updatable = false)
  private Long sourceRecordId;
  @Column(name = "award_name_snapshot", nullable = false, length = 100, updatable = false)
  private String awardName;
  @Enumerated(EnumType.STRING)
  @Column(name = "award_type_snapshot", nullable = false, length = 16, updatable = false)
  private AwardType awardType;
  @Column(name = "award_payload_snapshot", columnDefinition = "json", updatable = false)
  private String awardPayload;
  @Column(name = "obtained_at", nullable = false, updatable = false)
  private Instant obtainedAt;
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected UserAward() {}

  public UserAward(AwardIssuance issuance, Instant now) {
    userId = issuance.getUserId();
    awardId = issuance.getAwardId();
    issuanceId = issuance.getId();
    sourceType = issuance.getSourceType();
    sourceRecordId = issuance.getSourceRecordId();
    awardName = issuance.getAwardName();
    awardType = issuance.getAwardType();
    awardPayload = issuance.getAwardPayload();
    obtainedAt = now;
    createdAt = now;
  }

  public Long getId() { return id; }
  public Long getUserId() { return userId; }
  public Long getAwardId() { return awardId; }
  public Long getIssuanceId() { return issuanceId; }
  public AwardType getAwardType() { return awardType; }
  public Instant getObtainedAt() { return obtainedAt; }
}
