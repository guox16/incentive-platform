package com.incentive.activity.application.lottery;

import com.incentive.activity.domain.LotteryPoolEntry;
import com.incentive.activity.support.IncentiveBusinessException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class PrizeUnlockPreDrawRule implements LotteryPreDrawRule {
  public static final String TYPE = "PRIZE_UNLOCK";

  @Override
  public String type() {
    return TYPE;
  }

  @Override
  public void validateConfiguration(LotteryPreDrawRuleDefinition definition) {
    if (!(definition.parameters()
        instanceof LotteryPreDrawRuleDefinition.PrizeUnlockParameters parameters)) {
      throw invalid("解锁规则参数类型错误");
    }
    if (parameters.minimumDrawCounts().isEmpty()
        || parameters.minimumDrawCounts().entrySet().stream()
            .anyMatch(entry -> entry.getKey() == null || entry.getKey() <= 0
                || entry.getValue() == null || entry.getValue() < 1)) {
      throw invalid("解锁规则的奖品ID和抽奖次数必须为正整数");
    }
  }

  @Override
  public LotteryPreDrawRuleResult apply(
      LotteryPreDrawContext context, List<LotteryPoolEntry> pool,
      LotteryPreDrawRuleDefinition definition) {
    validateConfiguration(definition);
    var parameters = (LotteryPreDrawRuleDefinition.PrizeUnlockParameters) definition.parameters();
    List<LotteryPoolEntry> unlocked = pool.stream()
        .filter(entry -> context.drawNumber() >= parameters.minimumDrawCounts()
            .getOrDefault(entry.prize().getPrizeId(), 1L))
        .toList();
    return LotteryPreDrawRuleResult.continueWith(unlocked);
  }

  private IncentiveBusinessException invalid(String message) {
    return new IncentiveBusinessException(
        "LOTTERY_PRE_RULE_INVALID", message, HttpStatus.CONFLICT);
  }
}
