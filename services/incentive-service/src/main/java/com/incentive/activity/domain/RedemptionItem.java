package com.incentive.activity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "redemption_items")
public class RedemptionItem {
  public enum Status { ACTIVE, INACTIVE }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(name = "activity_id", nullable = false, updatable = false)
  private Long activityId;
  @Column(name = "rule_id", nullable = false, updatable = false)
  private Long ruleId;
  @Column(name = "item_code", nullable = false, length = 64, updatable = false)
  private String itemCode;
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
  @Column(name = "points_price", nullable = false, updatable = false)
  private long pointsPrice;
  @Column(name = "campaign_quota", updatable = false)
  private Long campaignQuota;
  @Column(name = "display_order", nullable = false, updatable = false)
  private int displayOrder;
  @Column(name = "eligibility_rule", columnDefinition = "json", updatable = false)
  private String eligibilityRule;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private Status status;

  protected RedemptionItem() {}

  public Long getId() { return id; }
  public Long getActivityId() { return activityId; }
  public Long getRuleId() { return ruleId; }
  public String getItemCode() { return itemCode; }
  public Long getPrizeId() { return prizeId; }
  public String getPrizeName() { return prizeName; }
  public PrizeType getPrizeType() { return prizeType; }
  public String getCoverUrl() { return coverUrl; }
  public String getAwardPayload() { return awardPayload; }
  public long getPointsPrice() { return pointsPrice; }
  public Long getCampaignQuota() { return campaignQuota; }
  public int getDisplayOrder() { return displayOrder; }
  public String getEligibilityRule() { return eligibilityRule; }
}
