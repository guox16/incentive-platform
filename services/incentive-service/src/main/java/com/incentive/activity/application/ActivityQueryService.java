package com.incentive.activity.application;

import com.incentive.activity.domain.ActivityStatus;
import com.incentive.activity.domain.ActivityType;
import com.incentive.activity.domain.IncentiveActivity;
import com.incentive.activity.domain.ParticipationRule;
import com.incentive.activity.dto.ActivityDetailResponse;
import com.incentive.activity.dto.ActivitySummaryResponse;
import com.incentive.activity.dto.LotteryPrizeResponse;
import com.incentive.activity.repository.IncentiveActivityRepository;
import com.incentive.activity.repository.LotteryPrizeRepository;
import com.incentive.activity.repository.ParticipationRuleRepository;
import com.incentive.activity.support.IncentiveBusinessException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ActivityQueryService {
  private final IncentiveActivityRepository activityRepository;
  private final ParticipationRuleRepository ruleRepository;
  private final LotteryPrizeRepository lotteryPrizeRepository;
  private final Clock clock;

  public ActivityQueryService(IncentiveActivityRepository activityRepository,
      ParticipationRuleRepository ruleRepository, LotteryPrizeRepository lotteryPrizeRepository,
      Clock clock) {
    this.activityRepository = activityRepository;
    this.ruleRepository = ruleRepository;
    this.lotteryPrizeRepository = lotteryPrizeRepository;
    this.clock = clock;
  }

  public List<ActivitySummaryResponse> activeActivities() {
    return activityRepository.findActive(ActivityStatus.ACTIVE,
            List.of(ActivityType.LOTTERY, ActivityType.REDEMPTION), clock.instant()).stream()
        .map(activity -> new ActivitySummaryResponse(activity.getId(), activity.getCode(),
            activity.getType(), activity.getName(), activity.getStartsAt(), activity.getEndsAt()))
        .toList();
  }

  public ActivityDetailResponse detail(String activityCode) {
    Instant now = clock.instant();
    IncentiveActivity activity = activityRepository.findByCode(activityCode)
        .orElseThrow(() -> notFound("活动不存在"));
    ensureActive(activity, now);
    ParticipationRule rule = findRule(activity.getId(), now);
    List<LotteryPrizeResponse> prizes = activity.getType() == ActivityType.LOTTERY
        ? lotteryPrizeRepository
            .findByActivityIdAndRuleIdOrderByDisplayOrderAscIdAsc(activity.getId(), rule.getId())
            .stream().map(prize -> new LotteryPrizeResponse(prize.getId(), prize.getPrizeId(),
                prize.getPrizeName(), prize.getPrizeType(), prize.getCoverUrl(),
                prize.getCampaignQuota(), prize.getDisplayOrder())).toList()
        : List.of();
    return new ActivityDetailResponse(activity.getId(), activity.getCode(), activity.getType(),
        activity.getName(), activity.getStatus(), activity.getStartsAt(), activity.getEndsAt(),
        rule.getRuleVersion(), rule.getPointsCost(), rule.getDailyLimit(), prizes);
  }

  public ParticipationRule findRule(Long activityId, Instant now) {
    return ruleRepository
        .findFirstByActivityIdAndStatusAndEffectiveFromLessThanEqualOrderByRuleVersionDesc(
            activityId, ParticipationRule.Status.ACTIVE, now)
        .orElseThrow(() -> new IncentiveBusinessException(
            "ACTIVITY_RULE_NOT_FOUND", "活动未配置生效规则", HttpStatus.CONFLICT));
  }

  public static void ensureActive(IncentiveActivity activity, Instant now) {
    boolean inTime = !activity.getStartsAt().isAfter(now)
        && (activity.getEndsAt() == null || activity.getEndsAt().isAfter(now));
    if (activity.getStatus() != ActivityStatus.ACTIVE || !inTime) {
      throw new IncentiveBusinessException(
          "ACTIVITY_NOT_ACTIVE", "活动当前不可参与", HttpStatus.CONFLICT);
    }
  }

  private IncentiveBusinessException notFound(String message) {
    return new IncentiveBusinessException("ACTIVITY_NOT_FOUND", message, HttpStatus.NOT_FOUND);
  }
}
