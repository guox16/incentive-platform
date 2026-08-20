package com.incentive.activity.application;

import com.incentive.activity.domain.ActivityType;
import com.incentive.activity.domain.IncentiveActivity;
import com.incentive.activity.domain.LotteryOrder;
import com.incentive.activity.domain.LotteryOrderStatus;
import com.incentive.activity.domain.LotteryPrize;
import com.incentive.activity.domain.ParticipationRule;
import com.incentive.activity.infrastructure.BusinessNumberGenerator;
import com.incentive.activity.infrastructure.LotteryPrizePicker;
import com.incentive.activity.repository.IncentiveActivityRepository;
import com.incentive.activity.repository.LotteryOrderRepository;
import com.incentive.activity.repository.LotteryPrizeRepository;
import com.incentive.activity.support.IncentiveBusinessException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LotteryOrderCreationService {
  private final IncentiveActivityRepository activityRepository;
  private final ActivityQueryService activityQueryService;
  private final LotteryPrizeRepository prizeRepository;
  private final LotteryOrderRepository orderRepository;
  private final LotteryPrizePicker prizePicker;
  private final BusinessNumberGenerator businessNumberGenerator;
  private final Clock clock;

  public LotteryOrderCreationService(IncentiveActivityRepository activityRepository,
      ActivityQueryService activityQueryService, LotteryPrizeRepository prizeRepository,
      LotteryOrderRepository orderRepository, LotteryPrizePicker prizePicker,
      BusinessNumberGenerator businessNumberGenerator, Clock clock) {
    this.activityRepository = activityRepository;
    this.activityQueryService = activityQueryService;
    this.prizeRepository = prizeRepository;
    this.orderRepository = orderRepository;
    this.prizePicker = prizePicker;
    this.businessNumberGenerator = businessNumberGenerator;
    this.clock = clock;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public CreationResult createOrGet(String activityCode, Long userId, String requestId) {
    String normalizedRequestId = normalizeRequestId(requestId);
    Instant now = clock.instant();
    IncentiveActivity activity = activityRepository.findByCodeForUpdate(activityCode)
        .orElseThrow(() -> new IncentiveBusinessException(
            "ACTIVITY_NOT_FOUND", "活动不存在", HttpStatus.NOT_FOUND));

    LotteryOrder existing = orderRepository
        .findByUserIdAndActivityIdAndRequestId(userId, activity.getId(), normalizedRequestId)
        .orElse(null);
    if (existing != null) return new CreationResult(existing, true);

    ActivityQueryService.ensureActive(activity, now);
    if (activity.getType() != ActivityType.LOTTERY) {
      throw new IncentiveBusinessException(
          "ACTIVITY_TYPE_MISMATCH", "该活动不是抽奖活动", HttpStatus.CONFLICT);
    }
    ParticipationRule rule = activityQueryService.findRule(activity.getId(), now);
    validateRule(rule);
    long usedToday = countToday(activity.getId(), userId);
    if (rule.getDailyLimit() != null && usedToday >= rule.getDailyLimit()) {
      throw new IncentiveBusinessException(
          "DAILY_LIMIT_REACHED", "今日参与次数已达上限", HttpStatus.CONFLICT);
    }

    List<LotteryPrize> prizes = prizeRepository
        .findByActivityIdAndRuleIdOrderByDisplayOrderAscIdAsc(activity.getId(), rule.getId());
    Long selectedId = prizePicker.pick(activity.getId(), rule.getRuleVersion(), prizes);
    LotteryPrize selected = prizes.stream().filter(prize -> prize.getId().equals(selectedId))
        .findFirst().orElseThrow(() -> new IncentiveBusinessException(
            "LOTTERY_POOL_STALE", "抽奖奖池与当前规则不一致", HttpStatus.CONFLICT));

    String eligibilityResult = "{\"passed\":true,\"usedTodayBefore\":" + usedToday + "}";
    LotteryOrder order = new LotteryOrder(
        businessNumberGenerator.next(), normalizedRequestId, userId, activity, rule, selected,
        businessNumberGenerator.next(), eligibilityResult, now);
    return new CreationResult(orderRepository.saveAndFlush(order), false);
  }

  private String normalizeRequestId(String requestId) {
    if (requestId == null || requestId.isBlank()) {
      throw new IncentiveBusinessException(
          "REQUEST_ID_REQUIRED", "requestId不能为空", HttpStatus.BAD_REQUEST);
    }
    String normalized = requestId.trim();
    if (normalized.length() > 64) {
      throw new IncentiveBusinessException(
          "REQUEST_ID_INVALID", "requestId长度不能超过64个字符", HttpStatus.BAD_REQUEST);
    }
    return normalized;
  }

  private void validateRule(ParticipationRule rule) {
    if (rule.getPointsCost() <= 0) {
      throw new IncentiveBusinessException(
          "LOTTERY_RULE_INVALID", "抽奖积分成本必须大于0", HttpStatus.CONFLICT);
    }
  }

  private long countToday(Long activityId, Long userId) {
    LocalDate today = LocalDate.now(clock);
    Instant from = today.atStartOfDay(clock.getZone()).toInstant();
    Instant to = today.plusDays(1).atStartOfDay(clock.getZone()).toInstant();
    return orderRepository
        .countByActivityIdAndUserIdAndStatusNotAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            activityId, userId, LotteryOrderStatus.FAILED, from, to);
  }

  public record CreationResult(LotteryOrder order, boolean replayed) {}
}
