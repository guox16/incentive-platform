package com.incentive.activity.application;

import com.incentive.activity.domain.ActivityType;
import com.incentive.activity.domain.IncentiveActivity;
import com.incentive.activity.domain.ParticipationRule;
import com.incentive.activity.domain.PendingAward;
import com.incentive.activity.domain.PrizeType;
import com.incentive.activity.domain.RedemptionItem;
import com.incentive.activity.domain.RedemptionRecord;
import com.incentive.activity.dto.RedemptionResponse;
import com.incentive.activity.infrastructure.BusinessNumberGenerator;
import com.incentive.activity.infrastructure.PointsClient;
import com.incentive.activity.repository.IncentiveActivityRepository;
import com.incentive.activity.repository.PendingAwardRepository;
import com.incentive.activity.repository.RedemptionItemRepository;
import com.incentive.activity.repository.RedemptionRecordRepository;
import com.incentive.activity.support.IncentiveBusinessException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RedemptionService {
  private final IncentiveActivityRepository activityRepository;
  private final ActivityQueryService activityQueryService;
  private final RedemptionItemRepository itemRepository;
  private final RedemptionRecordRepository recordRepository;
  private final PendingAwardRepository pendingAwardRepository;
  private final PointsClient pointsClient;
  private final BusinessNumberGenerator businessNumberGenerator;
  private final Clock clock;

  public RedemptionService(IncentiveActivityRepository activityRepository,
      ActivityQueryService activityQueryService, RedemptionItemRepository itemRepository,
      RedemptionRecordRepository recordRepository, PendingAwardRepository pendingAwardRepository,
      PointsClient pointsClient, BusinessNumberGenerator businessNumberGenerator, Clock clock) {
    this.activityRepository = activityRepository;
    this.activityQueryService = activityQueryService;
    this.itemRepository = itemRepository;
    this.recordRepository = recordRepository;
    this.pendingAwardRepository = pendingAwardRepository;
    this.pointsClient = pointsClient;
    this.businessNumberGenerator = businessNumberGenerator;
    this.clock = clock;
  }

  @Transactional
  public RedemptionResponse redeem(String activityCode, Long itemId, Long userId) {
    Instant now = clock.instant();
    IncentiveActivity activity = activityRepository.findByCodeForUpdate(activityCode)
        .orElseThrow(() -> new IncentiveBusinessException(
            "ACTIVITY_NOT_FOUND", "活动不存在", HttpStatus.NOT_FOUND));
    ActivityQueryService.ensureActive(activity, now);
    if (activity.getType() != ActivityType.REDEMPTION) {
      throw new IncentiveBusinessException(
          "ACTIVITY_TYPE_MISMATCH", "该活动不是兑换活动", HttpStatus.CONFLICT);
    }

    ParticipationRule rule = activityQueryService.findRule(activity.getId(), now);
    long usedToday = countToday(activity.getId(), userId);
    if (rule.getDailyLimit() != null && usedToday >= rule.getDailyLimit()) {
      throw new IncentiveBusinessException(
          "DAILY_LIMIT_REACHED", "今日兑换次数已达上限", HttpStatus.CONFLICT);
    }

    RedemptionItem item = itemRepository
        .findByIdAndActivityIdAndRuleIdAndStatus(
            itemId, activity.getId(), rule.getId(), RedemptionItem.Status.ACTIVE)
        .orElseThrow(() -> new IncentiveBusinessException(
            "REDEMPTION_ITEM_NOT_FOUND", "兑换商品不存在或已下架", HttpStatus.NOT_FOUND));
    validateItem(item);
    if (item.getCampaignQuota() != null
        && recordRepository.countByItemId(item.getId()) >= item.getCampaignQuota()) {
      throw new IncentiveBusinessException(
          "REDEMPTION_ITEM_SOLD_OUT", "兑换商品活动名额已用完", HttpStatus.CONFLICT);
    }

    PointsClient.PointDebitResult debit = pointsClient.debit(businessNumberGenerator.next(), userId,
        item.getPointsPrice(), "REDEMPTION", "兑换商品：" + item.getItemCode());
    String eligibilityResult = "{\"passed\":true,\"usedTodayBefore\":" + usedToday + "}";
    RedemptionRecord record = recordRepository.saveAndFlush(new RedemptionRecord(
        activity, rule, item, userId, eligibilityResult, debit.transactionId(), now));
    pendingAwardRepository.save(PendingAward.forRedemption(record, now));

    return new RedemptionResponse(record.getId(), activity.getCode(), item.getId(),
        item.getItemCode(), userId, item.getPrizeId(), item.getPrizeName(), item.getPrizeType(),
        item.getCoverUrl(), item.getPointsPrice(), debit.transactionId(), debit.balanceAfter(),
        true, now);
  }

  private void validateItem(RedemptionItem item) {
    if (item.getPointsPrice() <= 0 || item.getPrizeType() == PrizeType.NONE) {
      throw new IncentiveBusinessException(
          "REDEMPTION_ITEM_INVALID", "兑换商品配置无效", HttpStatus.CONFLICT);
    }
  }

  private long countToday(Long activityId, Long userId) {
    LocalDate today = LocalDate.now(clock);
    Instant from = today.atStartOfDay(clock.getZone()).toInstant();
    Instant to = today.plusDays(1).atStartOfDay(clock.getZone()).toInstant();
    return recordRepository
        .countByActivityIdAndUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            activityId, userId, from, to);
  }
}
