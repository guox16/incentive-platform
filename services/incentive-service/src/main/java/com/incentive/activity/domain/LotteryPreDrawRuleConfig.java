package com.incentive.activity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "lottery_pre_draw_rules", uniqueConstraints =
    @UniqueConstraint(name = "uk_lottery_pre_draw_rules_type",
        columnNames = {"participation_rule_id", "rule_type"}))
public class LotteryPreDrawRuleConfig {
  public static final String LUCKY_FALLBACK = "LUCKY_FALLBACK";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "activity_id", nullable = false, updatable = false)
  private Long activityId;

  @Column(name = "participation_rule_id", nullable = false, updatable = false)
  private Long participationRuleId;

  @Column(name = "rule_type", nullable = false, length = 64, updatable = false)
  private String ruleType;

  @Column(name = "execution_order", nullable = false, updatable = false)
  private int executionOrder;

  @Column(nullable = false, updatable = false)
  private boolean enabled;

  @Column(name = "rule_config", nullable = false, columnDefinition = "json", updatable = false)
  private String ruleConfig;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected LotteryPreDrawRuleConfig() {}

  public LotteryPreDrawRuleConfig(Long activityId, Long participationRuleId, String ruleType,
      int executionOrder, boolean enabled, String ruleConfig) {
    this.activityId = Objects.requireNonNull(activityId, "活动ID不能为空");
    this.participationRuleId = Objects.requireNonNull(participationRuleId, "参与规则ID不能为空");
    if (ruleType == null || ruleType.isBlank()) throw new IllegalArgumentException("规则类型不能为空");
    if (executionOrder < 0) throw new IllegalArgumentException("规则顺序不能小于0");
    if (ruleConfig == null || ruleConfig.isBlank()) throw new IllegalArgumentException("规则配置不能为空");
    this.ruleType = ruleType.trim();
    this.executionOrder = executionOrder;
    this.enabled = enabled;
    this.ruleConfig = ruleConfig;
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
  public Long getParticipationRuleId() { return participationRuleId; }
  public String getRuleType() { return ruleType; }
  public int getExecutionOrder() { return executionOrder; }
  public boolean isEnabled() { return enabled; }
  public String getRuleConfig() { return ruleConfig; }
}
