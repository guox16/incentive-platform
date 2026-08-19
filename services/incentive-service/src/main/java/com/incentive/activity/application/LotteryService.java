package com.incentive.activity.application;

import com.incentive.activity.domain.ActivityType;
import com.incentive.activity.domain.IncentiveActivity;
import com.incentive.activity.domain.LotteryParticipation;
import com.incentive.activity.domain.LotteryPrize;
import com.incentive.activity.domain.ParticipationRule;
import com.incentive.activity.domain.PendingAward;
import com.incentive.activity.domain.PrizeType;
import com.incentive.activity.dto.LotteryDrawResponse;
import com.incentive.activity.infrastructure.BusinessNumberGenerator;
import com.incentive.activity.infrastructure.LotteryPrizePicker;
import com.incentive.activity.infrastructure.PointsClient;
import com.incentive.activity.repository.IncentiveActivityRepository;
import com.incentive.activity.repository.LotteryParticipationRepository;
import com.incentive.activity.repository.LotteryPrizeRepository;
import com.incentive.activity.repository.PendingAwardRepository;
import com.incentive.activity.support.IncentiveBusinessException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class LotteryService {
  private final IncentiveActivityRepository activityRepository;
  private final ActivityQueryService activityQueryService;
  private final LotteryPrizeRepository prizeRepository;
  private final LotteryParticipationRepository participationRepository;
  private final PendingAwardRepository pendingAwardRepository;
  private final LotteryPrizePicker prizePicker;
  private final PointsClient pointsClient;
  private final BusinessNumberGenerator businessNumberGenerator;
  private final Clock clock;

  public LotteryService(IncentiveActivityRepository activityRepository,
      ActivityQueryService activityQueryService, LotteryPrizeRepository prizeRepository,
      LotteryParticipationRepository participationRepository,
      PendingAwardRepository pendingAwardRepository, LotteryPrizePicker prizePicker,
      PointsClient pointsClient, BusinessNumberGenerator businessNumberGenerator, Clock clock) {
    this.activityRepository = activityRepository;
    this.activityQueryService = activityQueryService;
    this.prizeRepository = prizeRepository;
    this.participationRepository = participationRepository;
    this.pendingAwardRepository = pendingAwardRepository;
    this.prizePicker = prizePicker;
    this.pointsClient = pointsClient;
    this.businessNumberGenerator = businessNumberGenerator;
    this.clock = clock;
  }

  @Transactional
  public LotteryDrawResponse draw(String activityCode, Long userId) {
    Instant now = clock.instant();
    IncentiveActivity activity = activityRepository.findByCodeForUpdate(activityCode)
        .orElseThrow(() -> new IncentiveBusinessException(
            "ACTIVITY_NOT_FOUND", "活动不存在", HttpStatus.NOT_FOUND));
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

    Long pointsBusinessId = businessNumberGenerator.next();
    PointsClient.PointReservationResult reservation = pointsClient.reserve(
        pointsBusinessId, userId, rule.getPointsCost(),
        "LOTTERY", "参与抽奖：" + activity.getCode());
    PointsClient.PointReservationResult confirmation =
        pointsClient.confirmReservation(pointsBusinessId);
    String eligibilityResult = "{\"passed\":true,\"usedTodayBefore\":" + usedToday + "}";
    LotteryParticipation participation = participationRepository.saveAndFlush(
        new LotteryParticipation(activity, rule, userId, selected, eligibilityResult,
            confirmation.confirmedTransactionId(), now));
    boolean createsPendingAward = selected.getPrizeType() != PrizeType.NONE;
    if (createsPendingAward) {
      pendingAwardRepository.save(PendingAward.forLottery(participation, now));
    }

    return new LotteryDrawResponse(participation.getId(), activity.getCode(), userId,
        selected.getPrizeId(), selected.getPrizeName(), selected.getPrizeType(),
        selected.getCoverUrl(), selected.getPrizeType() != PrizeType.NONE, createsPendingAward,
        rule.getPointsCost(), confirmation.confirmedTransactionId(), reservation.balanceAfter(), now);
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
    return participationRepository
        .countByActivityIdAndUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            activityId, userId, from, to);
  }
}
