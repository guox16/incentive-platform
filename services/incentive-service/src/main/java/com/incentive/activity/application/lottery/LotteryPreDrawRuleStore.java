package com.incentive.activity.application.lottery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.incentive.activity.domain.LotteryPreDrawRuleConfig;
import com.incentive.activity.repository.LotteryPreDrawRuleConfigRepository;
import com.incentive.activity.support.IncentiveBusinessException;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** 前置规则持久化模块。对调用方只暴露强类型定义，JSON 格式封装在模块内部。 */
@Service
public class LotteryPreDrawRuleStore {
  private static final TypeReference<LinkedHashSet<Long>> USER_IDS = new TypeReference<>() {};
  private static final TypeReference<LinkedHashMap<Long, Long>> UNLOCKS =
      new TypeReference<>() {};
  private static final TypeReference<LinkedHashMap<Long, LinkedHashMap<Long, BigDecimal>>>
      POINTS_WEIGHTS = new TypeReference<>() {};

  private final LotteryPreDrawRuleConfigRepository ruleRepository;
  private final ObjectMapper objectMapper;

  public LotteryPreDrawRuleStore(
      LotteryPreDrawRuleConfigRepository ruleRepository, ObjectMapper objectMapper) {
    this.ruleRepository = ruleRepository;
    this.objectMapper = objectMapper;
  }

  public List<LotteryPreDrawRuleDefinition> load(Long participationRuleId) {
    return ruleRepository.findByParticipationRuleIdOrderByExecutionOrderAscIdAsc(
        participationRuleId).stream().map(this::toDefinition).toList();
  }

  public void save(Long activityId, Long participationRuleId,
      List<LotteryPreDrawRuleDefinition> definitions) {
    List<LotteryPreDrawRuleConfig> rows = definitions.stream()
        .map(definition -> new LotteryPreDrawRuleConfig(
            activityId, participationRuleId, definition.type(), definition.executionOrder(),
            definition.enabled(), serialize(definition.parameters())))
        .toList();
    if (!rows.isEmpty()) ruleRepository.saveAll(rows);
  }

  private LotteryPreDrawRuleDefinition toDefinition(LotteryPreDrawRuleConfig stored) {
    LotteryPreDrawRuleDefinition.Parameters parameters = switch (stored.getRuleType()) {
      case UserListPreDrawRule.TYPE -> new LotteryPreDrawRuleDefinition.UserListParameters(
          read(stored.getRuleConfig(), USER_IDS));
      case PrizeUnlockPreDrawRule.TYPE ->
          new LotteryPreDrawRuleDefinition.PrizeUnlockParameters(
              read(stored.getRuleConfig(), UNLOCKS));
      case PointsWeightPreDrawRule.TYPE -> toPointsWeight(stored.getRuleConfig());
      case LotteryPreDrawRuleConfig.LUCKY_FALLBACK ->
          new LotteryPreDrawRuleDefinition.LuckyFallbackParameters(
              read(stored.getRuleConfig(), Long.class));
      default -> throw invalid("不支持的前置规则类型: " + stored.getRuleType());
    };
    return new LotteryPreDrawRuleDefinition(stored.getRuleType(), stored.getExecutionOrder(),
        stored.isEnabled(), parameters);
  }

  private LotteryPreDrawRuleDefinition.PointsWeightParameters toPointsWeight(String json) {
    Map<Long, LinkedHashMap<Long, BigDecimal>> configured = read(json, POINTS_WEIGHTS);
    return new LotteryPreDrawRuleDefinition.PointsWeightParameters(configured.entrySet().stream()
        .map(entry -> new LotteryPreDrawRuleDefinition.PointsTier(
            entry.getKey(), entry.getValue()))
        .toList());
  }

  private String serialize(LotteryPreDrawRuleDefinition.Parameters parameters) {
    Object value;
    if (parameters instanceof LotteryPreDrawRuleDefinition.UserListParameters users) {
      value = users.userIds();
    } else if (parameters
        instanceof LotteryPreDrawRuleDefinition.PrizeUnlockParameters unlocks) {
      value = unlocks.minimumDrawCounts();
    } else if (parameters
        instanceof LotteryPreDrawRuleDefinition.PointsWeightParameters points) {
      Map<Long, Map<Long, BigDecimal>> tiers = new LinkedHashMap<>();
      points.tiers().forEach(tier ->
          tiers.put(tier.minimumPoints(), tier.weightMultipliers()));
      value = tiers;
    } else if (parameters
        instanceof LotteryPreDrawRuleDefinition.LuckyFallbackParameters lucky) {
      value = lucky.prizeId();
    } else {
      throw invalid("不支持的前置规则参数类型");
    }
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException ex) {
      throw invalid("前置规则配置序列化失败");
    }
  }

  private <T> T read(String json, Class<T> type) {
    try {
      return objectMapper.readValue(json, type);
    } catch (JsonProcessingException ex) {
      throw invalid("数据库中的前置规则配置格式错误");
    }
  }

  private <T> T read(String json, TypeReference<T> type) {
    try {
      return objectMapper.readValue(json, type);
    } catch (JsonProcessingException ex) {
      throw invalid("数据库中的前置规则配置格式错误");
    }
  }

  private IncentiveBusinessException invalid(String message) {
    return new IncentiveBusinessException(
        "LOTTERY_PRE_RULE_INVALID", message, HttpStatus.CONFLICT);
  }
}
