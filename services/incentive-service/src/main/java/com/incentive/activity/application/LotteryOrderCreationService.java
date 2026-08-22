package com.incentive.activity.application;

import com.incentive.activity.application.lottery.LotteryPreDrawContext;
import com.incentive.activity.application.lottery.LotteryPostDrawStockRule;
import com.incentive.activity.application.lottery.LotteryPreDrawRuleChain;
import com.incentive.activity.application.lottery.LotteryPreDrawRuleDefinition;
import com.incentive.activity.application.lottery.LotteryPreDrawRuleStore;
import com.incentive.activity.domain.ActivityType;
import com.incentive.activity.domain.IncentiveActivity;
import com.incentive.activity.domain.LotteryOrder;
import com.incentive.activity.domain.LotteryOrderStatus;
import com.incentive.activity.domain.LotteryPreDrawRuleConfig;
import com.incentive.activity.domain.LotteryPrize;
import com.incentive.activity.domain.ParticipationRule;
import com.incentive.activity.infrastructure.BusinessNumberGenerator;
import com.incentive.activity.infrastructure.LotteryPrizePicker;
import com.incentive.activity.repository.IncentiveActivityRepository;
import com.incentive.activity.repository.LotteryOrderRepository;
import com.incentive.activity.repository.LotteryParticipationRepository;
import com.incentive.activity.repository.LotteryPrizeRepository;
import com.incentive.activity.support.IncentiveBusinessException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
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
  private final LotteryParticipationRepository participationRepository;
  private final LotteryPreDrawRuleStore preDrawRuleStore;
  private final LotteryPrizePicker prizePicker;
  private final LotteryPreDrawRuleChain preDrawRuleChain;
  private final LotteryPostDrawStockRule postDrawStockRule;
  private final BusinessNumberGenerator businessNumberGenerator;
  private final Clock clock;

  public LotteryOrderCreationService(IncentiveActivityRepository activityRepository,
      ActivityQueryService activityQueryService, LotteryPrizeRepository prizeRepository,
      LotteryOrderRepository orderRepository,
      LotteryParticipationRepository participationRepository,
      LotteryPreDrawRuleStore preDrawRuleStore,
      LotteryPrizePicker prizePicker, LotteryPreDrawRuleChain preDrawRuleChain,
      LotteryPostDrawStockRule postDrawStockRule,
      BusinessNumberGenerator businessNumberGenerator, Clock clock) {
    this.activityRepository = activityRepository;
    this.activityQueryService = activityQueryService;
    this.prizeRepository = prizeRepository;
    this.orderRepository = orderRepository;
    this.participationRepository = participationRepository;
    this.preDrawRuleStore = preDrawRuleStore;
    this.prizePicker = prizePicker;
    this.preDrawRuleChain = preDrawRuleChain;
    this.postDrawStockRule = postDrawStockRule;
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
    long drawNumber = Math.addExact(
        participationRepository.countByActivityIdAndUserId(activity.getId(), userId), 1L);
    var configuredRules = preDrawRuleStore.load(rule.getId());
    var resolution = preDrawRuleChain.resolve(configuredRules,
        new LotteryPreDrawContext(activity.getId(), userId, rule.getPointsCost(), drawNumber),
        prizes);
    LotteryPrize selected;
    if (resolution.designated()) {
      selected = resolution.designatedPrize();
    } else {
      Long selectedId = prizePicker.pick(
          activity.getId(), rule.getRuleVersion(), resolution.pool());
      selected = prizes.stream().filter(prize -> prize.getId().equals(selectedId))
          .findFirst().orElseThrow(() -> new IncentiveBusinessException(
              "LOTTERY_POOL_STALE", "抽奖奖池与当前规则不一致", HttpStatus.CONFLICT));
    }

    Long orderId = businessNumberGenerator.next();
    var stockResolution = postDrawStockRule.resolve(
        orderId, activity.getId(), selected, prizes, luckyPrizeId(configuredRules));
    LotteryPrize finalPrize = stockResolution.prize();
    try {
      String decision = resolution.designated()
          ? resolution.designatedBy() : "WEIGHTED_RANDOM";
      String eligibilityResult = "{\"passed\":true,\"usedTodayBefore\":" + usedToday
          + ",\"drawNumber\":" + drawNumber + ",\"preDrawDecision\":\"" + decision
          + "\",\"stockFallback\":" + stockResolution.fallback() + "}";
      LotteryOrder order = new LotteryOrder(
          orderId, normalizedRequestId, userId, activity, rule, finalPrize,
          businessNumberGenerator.next(), eligibilityResult, stockResolution.stockNo(), now);
      return new CreationResult(orderRepository.saveAndFlush(order), false);
    } catch (DataIntegrityViolationException failure) {
      postDrawStockRule.discard(orderId, activity.getId(), finalPrize.getPrizeId(),
          stockResolution.stockNo());
      throw failure;
    } catch (RuntimeException failure) {
      postDrawStockRule.release(orderId, activity.getId(), finalPrize.getPrizeId(),
          stockResolution.stockNo());
      throw failure;
    }
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

  private Long luckyPrizeId(List<LotteryPreDrawRuleDefinition> configuredRules) {
    return configuredRules.stream()
        .filter(rule -> LotteryPreDrawRuleConfig.LUCKY_FALLBACK.equals(rule.type()))
        .map(rule -> ((LotteryPreDrawRuleDefinition.LuckyFallbackParameters)
            rule.parameters()).prizeId())
        .findFirst().orElse(null);
  }

  public record CreationResult(LotteryOrder order, boolean replayed) {}
}
