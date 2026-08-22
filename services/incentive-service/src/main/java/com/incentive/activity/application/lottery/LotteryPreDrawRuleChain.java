package com.incentive.activity.application.lottery;

import com.incentive.activity.domain.LotteryPoolEntry;
import com.incentive.activity.domain.LotteryPreDrawRuleConfig;
import com.incentive.activity.domain.LotteryPrize;
import com.incentive.activity.domain.PrizeType;
import com.incentive.activity.support.IncentiveBusinessException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class LotteryPreDrawRuleChain {
  private final Map<String, LotteryPreDrawRule> rules;

  public LotteryPreDrawRuleChain(List<LotteryPreDrawRule> rules) {
    this.rules = new LinkedHashMap<>();
    rules.forEach(rule -> {
      if (this.rules.put(rule.type(), rule) != null) {
        throw new IllegalStateException("前置规则类型重复: " + rule.type());
      }
    });
  }

  public Resolution resolve(List<LotteryPreDrawRuleDefinition> configurations,
      LotteryPreDrawContext context, List<LotteryPrize> prizes) {
    if (prizes.isEmpty()) throw invalid("抽奖活动没有配置任何奖品");
    List<LotteryPoolEntry> basePool = prizes.stream().map(LotteryPoolEntry::original).toList();
    LotteryPreDrawRuleResult current = LotteryPreDrawRuleResult.continueWith(basePool);
    Long luckyPrizeId = null;

    for (LotteryPreDrawRuleDefinition configuredRule : configurations) {
      if (!configuredRule.enabled()) continue;
      if (LotteryPreDrawRuleConfig.LUCKY_FALLBACK.equals(configuredRule.type())) {
        if (!(configuredRule.parameters()
            instanceof LotteryPreDrawRuleDefinition.LuckyFallbackParameters lucky)) {
          throw invalid("幸运奖规则参数类型错误");
        }
        luckyPrizeId = lucky.prizeId();
        continue;
      }
      LotteryPreDrawRule rule = rules.get(configuredRule.type());
      if (rule == null) {
        throw invalid("不支持的前置规则类型: " + configuredRule.type());
      }
      rule.validateConfiguration(configuredRule);
      current = rule.apply(context, current.pool(), configuredRule);
      if (current.designated()) {
        return new Resolution(List.of(), current.designatedPrize(), configuredRule.type());
      }
    }
    if (!current.pool().isEmpty()) return new Resolution(current.pool(), null, null);
    return new Resolution(List.of(), findLuckyPrize(luckyPrizeId, prizes), "LUCKY_FALLBACK");
  }

  public void validateConfiguration(List<LotteryPreDrawRuleDefinition> definitions,
      Long luckyPrizeId) {
    Set<String> configuredTypes = new HashSet<>();
    for (LotteryPreDrawRuleDefinition definition : definitions) {
      if (definition.type() == null || definition.type().isBlank()) {
        throw invalid("前置规则type不能为空");
      }
      LotteryPreDrawRule rule = rules.get(definition.type());
      if (rule == null) throw invalid("不支持的前置规则类型: " + definition.type());
      if (!configuredTypes.add(definition.type())) {
        throw invalid("同一活动不能重复配置前置规则: " + definition.type());
      }
      rule.validateConfiguration(definition);
    }
    if (luckyPrizeId != null && luckyPrizeId <= 0) {
      throw invalid("luckyPrizeId必须是正整数");
    }
  }

  private LotteryPrize findLuckyPrize(Long configuredPrizeId, List<LotteryPrize> prizes) {
    if (configuredPrizeId != null) {
      return prizes.stream().filter(prize -> prize.getPrizeId().equals(configuredPrizeId))
          .findFirst().orElseThrow(() ->
              invalid("活动奖池中不存在幸运奖prizeId: " + configuredPrizeId));
    }
    return prizes.stream().filter(prize -> prize.getPrizeType() == PrizeType.NONE).findFirst()
        .orElseThrow(() -> invalid("奖池为空且活动未配置幸运奖"));
  }

  private IncentiveBusinessException invalid(String message) {
    return new IncentiveBusinessException(
        "LOTTERY_PRE_RULE_INVALID", message, HttpStatus.CONFLICT);
  }

  public record Resolution(
      List<LotteryPoolEntry> pool,
      LotteryPrize designatedPrize,
      String designatedBy) {
    public boolean designated() { return designatedPrize != null; }
  }
}
