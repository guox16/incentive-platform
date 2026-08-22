package com.incentive.activity.application.lottery;

import com.incentive.activity.domain.LotteryPoolEntry;
import com.incentive.activity.support.IncentiveBusinessException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * 积分权重节点。根据本次抽奖的积分成本选择不高于该成本的最高档位，
 * 再按奖品中心 prizeId 对候选奖品权重应用配置倍数，不负责增删奖品。
 */
@Component
public class PointsWeightPreDrawRule implements LotteryPreDrawRule {
  public static final String TYPE = "POINTS_WEIGHT";

  /** 返回活动配置中使用的规则类型标识。 */
  @Override
  public String type() {
    return TYPE;
  }

  /** 校验积分档位、奖品ID及权重倍数的结构和值域。 */
  @Override
  public void validateConfiguration(LotteryPreDrawRuleDefinition definition) {
    if (!(definition.parameters()
        instanceof LotteryPreDrawRuleDefinition.PointsWeightParameters parameters)) {
      throw invalid("积分权重规则参数类型错误");
    }
    if (parameters.tiers().isEmpty()) throw invalid("积分权重规则必须配置积分档位");
    java.util.Set<Long> minimumPoints = new java.util.HashSet<>();
    for (LotteryPreDrawRuleDefinition.PointsTier tier : parameters.tiers()) {
      if (tier.minimumPoints() < 0 || !minimumPoints.add(tier.minimumPoints())) {
        throw invalid("积分档位必须非负且不能重复");
      }
      if (tier.weightMultipliers().isEmpty()
          || tier.weightMultipliers().entrySet().stream()
              .anyMatch(entry -> entry.getKey() == null || entry.getKey() <= 0
                  || entry.getValue() == null || entry.getValue().signum() <= 0)) {
        throw invalid("积分档位的奖品ID和权重倍数必须为正数");
      }
    }
  }

  /**
   * 选择当前积分成本可以命中的最高档位，并生成调整权重后的新候选奖池。
   */
  @Override
  public LotteryPreDrawRuleResult apply(
      LotteryPreDrawContext context, List<LotteryPoolEntry> pool,
      LotteryPreDrawRuleDefinition definition) {
    validateConfiguration(definition);
    var parameters = (LotteryPreDrawRuleDefinition.PointsWeightParameters) definition.parameters();
    LotteryPreDrawRuleDefinition.PointsTier matched = null;
    long matchedMinimum = Long.MIN_VALUE;
    // 档位可以无序配置，这里显式选择满足条件的最高档位。
    for (LotteryPreDrawRuleDefinition.PointsTier tier : parameters.tiers()) {
      long value = tier.minimumPoints();
      if (value <= context.pointsCost() && value > matchedMinimum) {
        matched = tier;
        matchedMinimum = value;
      }
    }
    if (matched == null) return LotteryPreDrawRuleResult.continueWith(pool);
    var multipliers = matched.weightMultipliers();

    List<LotteryPoolEntry> adjusted = new ArrayList<>(pool.size());
    for (LotteryPoolEntry entry : pool) {
      BigDecimal multiplier = multipliers.get(entry.prize().getPrizeId());
      if (multiplier == null) {
        adjusted.add(entry);
        continue;
      }
      try {
        // 权重必须保持为正整数；小数结果按四舍五入转换，最低保留为1。
        long weight = BigDecimal.valueOf(entry.weight()).multiply(multiplier)
            .setScale(0, RoundingMode.HALF_UP).max(BigDecimal.ONE).longValueExact();
        adjusted.add(new LotteryPoolEntry(entry.prize(), weight));
      } catch (ArithmeticException ex) {
        throw invalid("奖品" + entry.prize().getPrizeId() + "调整后的权重超出范围");
      }
    }
    return LotteryPreDrawRuleResult.continueWith(adjusted);
  }

  /** 创建统一的前置规则配置异常。 */
  private IncentiveBusinessException invalid(String message) {
    return new IncentiveBusinessException(
        "LOTTERY_PRE_RULE_INVALID", message, HttpStatus.CONFLICT);
  }
}
