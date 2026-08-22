package com.incentive.activity.application.lottery;

import com.incentive.activity.domain.LotteryPoolEntry;
import com.incentive.activity.support.IncentiveBusinessException;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * 用户名单节点。活动通过 USER_LIST 配置用户ID；命中后直接指定当前权重最大的奖品，
 * 返回指定结果以终止后续前置规则，未命中则原样传递奖池。
 */
@Component
public class UserListPreDrawRule implements LotteryPreDrawRule {
  public static final String TYPE = "USER_LIST";

  /** 返回活动配置中使用的规则类型标识。 */
  @Override
  public String type() {
    return TYPE;
  }

  /** 校验名单用户ID和动作类型，避免无效配置进入抽奖流程。 */
  @Override
  public void validateConfiguration(LotteryPreDrawRuleDefinition definition) {
    if (!(definition.parameters()
        instanceof LotteryPreDrawRuleDefinition.UserListParameters parameters)) {
      throw invalid("名单规则参数类型错误");
    }
    if (parameters.userIds().isEmpty()
        || parameters.userIds().stream().anyMatch(id -> id == null || id <= 0)) {
      throw invalid("名单规则必须配置正整数用户ID");
    }
  }

  /**
   * 匹配当前用户；未命中时继续传递奖池，命中时指定最大权重奖品并短路责任链。
   */
  @Override
  public LotteryPreDrawRuleResult apply(
      LotteryPreDrawContext context, List<LotteryPoolEntry> pool,
      LotteryPreDrawRuleDefinition definition) {
    validateConfiguration(definition);
    var parameters = (LotteryPreDrawRuleDefinition.UserListParameters) definition.parameters();

    boolean matched = parameters.userIds().contains(context.userId());
    if (!matched) return LotteryPreDrawRuleResult.continueWith(pool);

    // 权重相同时优先展示顺序靠前、活动奖品ID较小的奖品，保证选择结果稳定。
    LotteryPoolEntry selected = pool.stream().max(Comparator
        .comparingLong(LotteryPoolEntry::weight)
        .thenComparing(entry -> -entry.prize().getDisplayOrder())
        .thenComparing(entry -> -entry.lotteryPrizeId()))
        .orElseThrow(() -> invalid("名单规则命中时基础奖池不能为空"));
    return LotteryPreDrawRuleResult.designate(pool, selected.prize());
  }

  /** 创建统一的前置规则配置异常。 */
  private IncentiveBusinessException invalid(String message) {
    return new IncentiveBusinessException(
        "LOTTERY_PRE_RULE_INVALID", message, HttpStatus.CONFLICT);
  }
}
