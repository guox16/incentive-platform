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
@Table(name = "lottery_prizes")
public class LotteryPrize {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "activity_id", nullable = false, updatable = false)
  private Long activityId;

  @Column(name = "rule_id", nullable = false, updatable = false)
  private Long ruleId;

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

  @Column(nullable = false, updatable = false)
  private long weight;

  @Column(name = "campaign_quota", updatable = false)
  private Long campaignQuota;

  @Column(name = "display_order", nullable = false, updatable = false)
  private int displayOrder;

  protected LotteryPrize() {}

  public LotteryPrize copyToRule(Long newRuleId) {
    LotteryPrize copy = new LotteryPrize();
    copy.activityId = activityId;
    copy.ruleId = newRuleId;
    copy.prizeId = prizeId;
    copy.prizeName = prizeName;
    copy.prizeType = prizeType;
    copy.coverUrl = coverUrl;
    copy.awardPayload = awardPayload;
    copy.weight = weight;
    copy.campaignQuota = campaignQuota;
    copy.displayOrder = displayOrder;
    return copy;
  }

  public Long getId() { return id; }
  public Long getActivityId() { return activityId; }
  public Long getRuleId() { return ruleId; }
  public Long getPrizeId() { return prizeId; }
  public String getPrizeName() { return prizeName; }
  public PrizeType getPrizeType() { return prizeType; }
  public String getCoverUrl() { return coverUrl; }
  public String getAwardPayload() { return awardPayload; }
  public long getWeight() { return weight; }
  public Long getCampaignQuota() { return campaignQuota; }
  public int getDisplayOrder() { return displayOrder; }
}
