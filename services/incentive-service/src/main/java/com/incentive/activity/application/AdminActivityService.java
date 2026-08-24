package com.incentive.activity.application;

import com.incentive.activity.application.lottery.LotteryPreDrawRuleChain;
import com.incentive.activity.application.lottery.LotteryPreDrawRuleDefinition;
import com.incentive.activity.application.lottery.LotteryPreDrawRuleStore;
import com.incentive.activity.application.lottery.PointsWeightPreDrawRule;
import com.incentive.activity.application.lottery.PrizeUnlockPreDrawRule;
import com.incentive.activity.application.lottery.UserListPreDrawRule;
import com.incentive.activity.domain.ActivityStatus;
import com.incentive.activity.domain.ActivityType;
import com.incentive.activity.domain.IncentiveActivity;
import com.incentive.activity.domain.LotteryPreDrawRuleConfig;
import com.incentive.activity.domain.ParticipationRule;
import com.incentive.activity.dto.AdminActivityResponse;
import com.incentive.activity.dto.CreateActivityRequest;
import com.incentive.activity.dto.LotteryPreDrawRuleRequest;
import com.incentive.activity.dto.LotteryPreDrawRuleResponse;
import com.incentive.activity.dto.UpdateActivityRequest;
import com.incentive.activity.infrastructure.BusinessNumberGenerator;
import com.incentive.activity.repository.IncentiveActivityRepository;
import com.incentive.activity.repository.LotteryPrizeRepository;
import com.incentive.activity.repository.ParticipationRuleRepository;
import com.incentive.activity.repository.LotteryPreDrawRuleConfigRepository;
import com.incentive.activity.repository.RedemptionItemRepository;
import com.incentive.activity.repository.RedemptionRecordRepository;
import com.incentive.activity.repository.LotteryOrderRepository;
import com.incentive.activity.support.IncentiveBusinessException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminActivityService {
  private final IncentiveActivityRepository activityRepository;
  private final ParticipationRuleRepository ruleRepository;
  private final LotteryPreDrawRuleStore preDrawRuleStore;
  private final LotteryPrizeRepository lotteryPrizeRepository;
  private final LotteryPreDrawRuleConfigRepository preDrawRuleConfigRepository;
  private final RedemptionItemRepository redemptionItemRepository;
  private final RedemptionRecordRepository redemptionRecordRepository;
  private final LotteryOrderRepository lotteryOrderRepository;
  private final LotteryPreDrawRuleChain preDrawRuleChain;
  private final BusinessNumberGenerator businessNumberGenerator;
  private final Clock clock;

  public AdminActivityService(IncentiveActivityRepository activityRepository,
      ParticipationRuleRepository ruleRepository,
      LotteryPreDrawRuleStore preDrawRuleStore,
      LotteryPrizeRepository lotteryPrizeRepository,
      LotteryPreDrawRuleConfigRepository preDrawRuleConfigRepository,
      RedemptionItemRepository redemptionItemRepository,
      RedemptionRecordRepository redemptionRecordRepository,
      LotteryOrderRepository lotteryOrderRepository,
      LotteryPreDrawRuleChain preDrawRuleChain,
      BusinessNumberGenerator businessNumberGenerator, Clock clock) {
    this.activityRepository = activityRepository;
    this.ruleRepository = ruleRepository;
    this.preDrawRuleStore = preDrawRuleStore;
    this.lotteryPrizeRepository = lotteryPrizeRepository;
    this.preDrawRuleConfigRepository = preDrawRuleConfigRepository;
    this.redemptionItemRepository = redemptionItemRepository;
    this.redemptionRecordRepository = redemptionRecordRepository;
    this.lotteryOrderRepository = lotteryOrderRepository;
    this.preDrawRuleChain = preDrawRuleChain;
    this.businessNumberGenerator = businessNumberGenerator;
    this.clock = clock;
  }

  public List<AdminActivityResponse> list(ActivityType type, ActivityStatus status) {
    return activityRepository.findAll(Sort.by(Sort.Direction.DESC, "updatedAt", "id")).stream()
        .filter(activity -> activity.getType() != ActivityType.CHECK_IN)
        .filter(activity -> type == null || activity.getType() == type)
        .filter(activity -> status == null || activity.getStatus() == status)
        .map(this::response)
        .toList();
  }

  public AdminActivityResponse get(Long id) {
    return response(find(id));
  }

  @Transactional
  public AdminActivityResponse create(CreateActivityRequest request) {
    ensureManageable(request.type());
    validateTime(request.startsAt(), request.endsAt());
    List<LotteryPreDrawRuleRequest> preDrawRules = rulesOrEmpty(request.preDrawRules());
    validateRuleSettings(request.type(), request.luckyPrizeId(), preDrawRules);
    String code = nextCode(request.type());
    IncentiveActivity activity = activityRepository.save(new IncentiveActivity(
        code, request.type(), request.name().trim(), request.startsAt(), request.endsAt()));
    ParticipationRule rule = ruleRepository.saveAndFlush(new ParticipationRule(
        activity.getId(), 1, request.pointsCost(),
        dailyLimit(request.type(), request.dailyLimit()), clock.instant()));
    savePreDrawRules(activity, rule, request.luckyPrizeId(), preDrawRules);
    return response(activity);
  }

  @Transactional
  public AdminActivityResponse update(Long id, UpdateActivityRequest request) {
    IncentiveActivity activity = find(id);
    ensureManageable(activity.getType());
    validateTime(request.startsAt(), request.endsAt());
    List<LotteryPreDrawRuleRequest> preDrawRules = rulesOrEmpty(request.preDrawRules());
    validateRuleSettings(activity.getType(), request.luckyPrizeId(), preDrawRules);
    ParticipationRule currentRule = latestRule(activity.getId());
    RuleSettings requestedSettings = new RuleSettings(request.luckyPrizeId(), preDrawRules);
    Integer requestedDailyLimit = dailyLimit(activity.getType(), request.dailyLimit());
    if (activity.getType() == ActivityType.LOTTERY && request.status() == ActivityStatus.ACTIVE
        && lotteryPrizeRepository.findByActivityIdAndRuleIdOrderByDisplayOrderAscIdAsc(
            activity.getId(), currentRule.getId()).isEmpty()) {
      throw conflict("PRIZE_POOL_EMPTY", "启用抽奖活动前请先配置奖池");
    }
    activity.update(request.name().trim(), request.status(), request.startsAt(), request.endsAt());
    if (ruleChanged(currentRule, request, requestedDailyLimit, requestedSettings)) {
      ParticipationRule newRule = ruleRepository.saveAndFlush(new ParticipationRule(
          activity.getId(), currentRule.getRuleVersion() + 1, request.pointsCost(),
          requestedDailyLimit, clock.instant()));
      copyLotteryPrizes(activity, currentRule, newRule);
      savePreDrawRules(activity, newRule, request.luckyPrizeId(), preDrawRules);
    }
    return response(activity);
  }

  @Transactional
  public void delete(Long id) {
    IncentiveActivity activity = find(id);
    ensureManageable(activity.getType());
    if (activity.getStatus() != ActivityStatus.ENDED) {
      throw conflict("ACTIVITY_NOT_ENDED", "请先结束活动，再执行删除操作");
    }
    if (lotteryOrderRepository.countByActivityId(id) > 0
        || redemptionRecordRepository.countByActivityId(id) > 0) {
      throw conflict("ACTIVITY_HAS_PARTICIPATION", "已有参与记录的活动不能删除");
    }
    preDrawRuleConfigRepository.deleteByActivityId(id);
    lotteryPrizeRepository.deleteByActivityId(id);
    redemptionItemRepository.deleteByActivityId(id);
    ruleRepository.deleteByActivityId(id);
    activityRepository.delete(activity);
  }

  private void copyLotteryPrizes(IncentiveActivity activity, ParticipationRule currentRule,
      ParticipationRule newRule) {
    if (activity.getType() != ActivityType.LOTTERY) return;
    var copies = lotteryPrizeRepository
        .findByActivityIdAndRuleIdOrderByDisplayOrderAscIdAsc(
            activity.getId(), currentRule.getId()).stream()
        .map(prize -> prize.copyToRule(newRule.getId()))
        .toList();
    if (!copies.isEmpty()) lotteryPrizeRepository.saveAll(copies);
  }

  private void savePreDrawRules(IncentiveActivity activity, ParticipationRule participationRule,
      Long luckyPrizeId, List<LotteryPreDrawRuleRequest> rules) {
    if (activity.getType() != ActivityType.LOTTERY) return;
    List<LotteryPreDrawRuleDefinition> definitions = new ArrayList<>();
    for (int index = 0; index < rules.size(); index++) {
      definitions.add(toDefinition(rules.get(index), (index + 1) * 10));
    }
    if (luckyPrizeId != null) {
      definitions.add(new LotteryPreDrawRuleDefinition(LotteryPreDrawRuleConfig.LUCKY_FALLBACK,
          Integer.MAX_VALUE, true,
          new LotteryPreDrawRuleDefinition.LuckyFallbackParameters(luckyPrizeId)));
    }
    if (!definitions.isEmpty()) {
      preDrawRuleStore.save(activity.getId(), participationRule.getId(), definitions);
    }
  }

  private AdminActivityResponse response(IncentiveActivity activity) {
    ParticipationRule rule = latestRule(activity.getId());
    RuleSettings settings = readRuleSettings(rule.getId());
    List<LotteryPreDrawRuleResponse> responses = new ArrayList<>();
    List<LotteryPreDrawRuleRequest> configuredRules = settings.rules();
    for (int index = 0; index < configuredRules.size(); index++) {
      LotteryPreDrawRuleRequest configured = configuredRules.get(index);
      responses.add(new LotteryPreDrawRuleResponse(
          configured.type(), (index + 1) * 10, configured.enabled(), configured.userIds(),
          configured.prizeMinimumDrawCounts(), configured.pointsTiers().stream()
              .map(tier -> new LotteryPreDrawRuleResponse.PointsTier(
                  tier.minimumPoints(), tier.weightMultipliers()))
              .toList()));
    }
    return new AdminActivityResponse(activity.getId(), activity.getCode(), activity.getType(),
        activity.getName(), activity.getStatus(), activity.getStartsAt(), activity.getEndsAt(),
        rule.getRuleVersion(), rule.getPointsCost(), dailyLimit(activity.getType(),
            rule.getDailyLimit()), settings.luckyPrizeId(),
        List.copyOf(responses), activity.getCreatedAt(), activity.getUpdatedAt());
  }

  private RuleSettings readRuleSettings(Long participationRuleId) {
    if (participationRuleId == null) return new RuleSettings(null, List.of());
    Long luckyPrizeId = null;
    List<LotteryPreDrawRuleRequest> configuredRules = new ArrayList<>();
    for (LotteryPreDrawRuleDefinition stored : preDrawRuleStore.load(participationRuleId)) {
      if (LotteryPreDrawRuleConfig.LUCKY_FALLBACK.equals(stored.type())) {
        luckyPrizeId = ((LotteryPreDrawRuleDefinition.LuckyFallbackParameters)
            stored.parameters()).prizeId();
      } else {
        configuredRules.add(toRequest(stored));
      }
    }
    return new RuleSettings(luckyPrizeId, List.copyOf(configuredRules));
  }

  private void validateRuleSettings(ActivityType type, Long luckyPrizeId,
      List<LotteryPreDrawRuleRequest> rules) {
    if (type != ActivityType.LOTTERY) {
      if (luckyPrizeId != null || !rules.isEmpty()) {
        throw new IncentiveBusinessException("LOTTERY_RULE_TYPE_MISMATCH",
            "只有抽奖活动可以配置抽奖前置规则", HttpStatus.BAD_REQUEST);
      }
      return;
    }
    List<LotteryPreDrawRuleDefinition> definitions = new ArrayList<>();
    for (int index = 0; index < rules.size(); index++) {
      definitions.add(toDefinition(rules.get(index), (index + 1) * 10));
    }
    preDrawRuleChain.validateConfiguration(definitions, luckyPrizeId);
  }

  private LotteryPreDrawRuleDefinition toDefinition(
      LotteryPreDrawRuleRequest request, int executionOrder) {
    LotteryPreDrawRuleDefinition.Parameters parameters = switch (request.type()) {
      case UserListPreDrawRule.TYPE -> new LotteryPreDrawRuleDefinition.UserListParameters(
          new LinkedHashSet<>(request.userIds() == null ? List.of() : request.userIds()));
      case PrizeUnlockPreDrawRule.TYPE ->
          new LotteryPreDrawRuleDefinition.PrizeUnlockParameters(
              request.prizeMinimumDrawCounts() == null
                  ? Map.of() : request.prizeMinimumDrawCounts());
      case PointsWeightPreDrawRule.TYPE ->
          new LotteryPreDrawRuleDefinition.PointsWeightParameters(
              (request.pointsTiers() == null ? List.<LotteryPreDrawRuleRequest.PointsTier>of()
                  : request.pointsTiers()).stream()
                  .map(tier -> new LotteryPreDrawRuleDefinition.PointsTier(
                      tier.minimumPoints(), tier.weightMultipliers() == null
                          ? Map.of() : tier.weightMultipliers()))
                  .toList());
      default -> throw conflict("LOTTERY_PRE_RULE_INVALID",
          "不支持的前置规则类型: " + request.type());
    };
    return new LotteryPreDrawRuleDefinition(
        request.type(), executionOrder, request.enabled(), parameters);
  }

  private LotteryPreDrawRuleRequest toRequest(LotteryPreDrawRuleDefinition definition) {
    List<Long> userIds = List.of();
    Map<Long, Long> unlocks = Map.of();
    List<LotteryPreDrawRuleRequest.PointsTier> tiers = List.of();
    if (definition.parameters() instanceof LotteryPreDrawRuleDefinition.UserListParameters users) {
      userIds = users.userIds().stream().sorted().toList();
    } else if (definition.parameters()
        instanceof LotteryPreDrawRuleDefinition.PrizeUnlockParameters configuredUnlocks) {
      unlocks = configuredUnlocks.minimumDrawCounts();
    } else if (definition.parameters()
        instanceof LotteryPreDrawRuleDefinition.PointsWeightParameters configuredPoints) {
      tiers = configuredPoints.tiers().stream()
          .map(tier -> new LotteryPreDrawRuleRequest.PointsTier(
              tier.minimumPoints(), tier.weightMultipliers()))
          .toList();
    }
    return new LotteryPreDrawRuleRequest(
        definition.type(), definition.enabled(), userIds, unlocks, tiers);
  }

  private List<LotteryPreDrawRuleRequest> rulesOrEmpty(List<LotteryPreDrawRuleRequest> rules) {
    if (rules == null) return List.of();
    List<LotteryPreDrawRuleRequest> normalized = new ArrayList<>();
    for (int index = 0; index < rules.size(); index++) {
      normalized.add(toRequest(toDefinition(rules.get(index), (index + 1) * 10)));
    }
    return List.copyOf(normalized);
  }

  private IncentiveActivity find(Long id) {
    return activityRepository.findById(id).orElseThrow(() ->
        new IncentiveBusinessException("ACTIVITY_NOT_FOUND", "活动不存在", HttpStatus.NOT_FOUND));
  }

  private ParticipationRule latestRule(Long activityId) {
    return ruleRepository.findFirstByActivityIdOrderByRuleVersionDesc(activityId)
        .orElseThrow(() -> conflict("ACTIVITY_RULE_NOT_FOUND", "活动未配置参与规则"));
  }

  private void ensureManageable(ActivityType type) {
    if (type == ActivityType.CHECK_IN) {
      throw new IncentiveBusinessException("ACTIVITY_TYPE_NOT_MANAGEABLE",
          "签到活动请使用签到规则管理", HttpStatus.BAD_REQUEST);
    }
  }

  private void validateTime(Instant startsAt, Instant endsAt) {
    if (endsAt != null && !endsAt.isAfter(startsAt)) {
      throw new IncentiveBusinessException("ACTIVITY_TIME_INVALID",
          "结束时间必须晚于开始时间", HttpStatus.BAD_REQUEST);
    }
  }

  private boolean ruleChanged(ParticipationRule currentRule, UpdateActivityRequest request,
      Integer requestedDailyLimit, RuleSettings requestedSettings) {
    return currentRule.getPointsCost() != request.pointsCost()
        || !Objects.equals(currentRule.getDailyLimit(), requestedDailyLimit)
        || !readRuleSettings(currentRule.getId()).equals(requestedSettings);
  }

  private Integer dailyLimit(ActivityType type, Integer requestedDailyLimit) {
    return type == ActivityType.LOTTERY ? requestedDailyLimit : null;
  }

  private String nextCode(ActivityType type) {
    return type.name() + "_"
        + Long.toUnsignedString(businessNumberGenerator.next(), 36).toUpperCase(Locale.ROOT);
  }

  private IncentiveBusinessException conflict(String code, String message) {
    return new IncentiveBusinessException(code, message, HttpStatus.CONFLICT);
  }

  private record RuleSettings(Long luckyPrizeId, List<LotteryPreDrawRuleRequest> rules) {}
}
