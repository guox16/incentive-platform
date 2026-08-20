package com.incentive.activity.application;

import com.incentive.activity.domain.ActivityType;
import com.incentive.activity.domain.IncentiveActivity;
import com.incentive.activity.domain.ParticipationRule;
import com.incentive.activity.domain.PendingAward;
import com.incentive.activity.domain.PrizeType;
import com.incentive.activity.domain.RedemptionItem;
import com.incentive.activity.domain.RedemptionRecord;
import com.incentive.activity.domain.RedemptionStatus;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 兑换的本地事务边界；远程积分扣减发生在两个事务之间。 */
@Service
public class RedemptionTransactions {
  private final IncentiveActivityRepository activityRepository;
  private final ActivityQueryService activityQueryService;
  private final RedemptionItemRepository itemRepository;
  private final RedemptionRecordRepository recordRepository;
  private final PendingAwardRepository pendingAwardRepository;
  private final Clock clock;

  public RedemptionTransactions(IncentiveActivityRepository activityRepository,
      ActivityQueryService activityQueryService, RedemptionItemRepository itemRepository,
      RedemptionRecordRepository recordRepository, PendingAwardRepository pendingAwardRepository,
      Clock clock) {
    this.activityRepository = activityRepository;
    this.activityQueryService = activityQueryService;
    this.itemRepository = itemRepository;
    this.recordRepository = recordRepository;
    this.pendingAwardRepository = pendingAwardRepository;
    this.clock = clock;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public RedemptionRecord createPending(String requestId, String activityCode, Long itemId,
      Long userId, Long pointBusinessId) {
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

    String eligibilityResult = "{\"passed\":true,\"usedTodayBefore\":" + usedToday + "}";
    return recordRepository.saveAndFlush(new RedemptionRecord(
        requestId, activity, rule, item, userId, eligibilityResult, pointBusinessId, now));
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public RedemptionRecord complete(Long redemptionId, Long transactionId, long balanceAfter) {
    RedemptionRecord record = recordRepository.findByIdForUpdate(redemptionId)
        .orElseThrow(() -> new IncentiveBusinessException(
            "REDEMPTION_NOT_FOUND", "兑换记录不存在", HttpStatus.NOT_FOUND));
    if (record.getStatus() == RedemptionStatus.COMPLETED) return record;
    Instant now = clock.instant();
    record.complete(transactionId, balanceAfter, now);
    pendingAwardRepository.save(PendingAward.forRedemption(record, now));
    return record;
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
