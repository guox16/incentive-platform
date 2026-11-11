package com.incentive.activity.application;

import com.incentive.activity.domain.IncentiveActivity;
import com.incentive.activity.domain.LotteryOrder;
import com.incentive.activity.domain.LotteryOrderStatus;
import com.incentive.activity.domain.PendingAward;
import com.incentive.activity.domain.PrizeType;
import com.incentive.activity.dto.LotteryRecordResponse;
import com.incentive.activity.dto.LotteryRecordPageResponse;
import com.incentive.activity.dto.LotteryRecordStatus;
import com.incentive.activity.repository.IncentiveActivityRepository;
import com.incentive.activity.repository.LotteryOrderRepository;
import com.incentive.activity.repository.PendingAwardRepository;
import com.incentive.activity.repository.PendingAwardRepository.LotteryAwardProgress;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LotteryRecordQueryService {
  private final LotteryOrderRepository orderRepository;
  private final IncentiveActivityRepository activityRepository;
  private final PendingAwardRepository awardRepository;
  private final int maxAwardRetries;

  public LotteryRecordQueryService(LotteryOrderRepository orderRepository,
      IncentiveActivityRepository activityRepository, PendingAwardRepository awardRepository,
      @Value("${award.dispatch.max-retries:8}") int maxAwardRetries) {
    this.orderRepository = orderRepository;
    this.activityRepository = activityRepository;
    this.awardRepository = awardRepository;
    this.maxAwardRetries = maxAwardRetries;
  }

  /** 抽奖单覆盖尚未生成参与记录的中间态，因此记录查询以抽奖单为准。 */
  @Transactional(readOnly = true)
  public LotteryRecordPageResponse findByUser(Long userId, int page, int size) {
    var result = orderRepository.findByUserIdOrderByCreatedAtDesc(
        userId, PageRequest.of(page, size));
    var orders = result.getContent();
    Map<Long, IncentiveActivity> activities = activityRepository.findAllById(
            orders.stream().map(LotteryOrder::getActivityId).distinct().toList())
        .stream().collect(Collectors.toMap(IncentiveActivity::getId, Function.identity()));
    var winningOrderIds = orders.stream()
        .filter(order -> order.getStatus() == LotteryOrderStatus.SUCCESS
            && order.getPrizeType() != PrizeType.NONE)
        .map(LotteryOrder::getId).toList();
    Map<Long, LotteryRecordResponse.AwardStatus> awardStatuses = winningOrderIds.isEmpty()
        ? Map.of()
        : awardRepository.findLotteryAwardProgress(userId, PendingAward.SourceType.LOTTERY, winningOrderIds)
            .stream().collect(Collectors.toMap(LotteryAwardProgress::getOrderId, this::awardStatus));
    var items = orders.stream()
        .map(order -> toResponse(order, activities.get(order.getActivityId()), awardStatuses.get(order.getId())))
        .toList();
    return new LotteryRecordPageResponse(items, result.getNumber(), result.getSize(),
        result.getTotalElements(), result.getTotalPages());
  }

  private LotteryRecordResponse toResponse(
      LotteryOrder order, IncentiveActivity activity, LotteryRecordResponse.AwardStatus awardStatus) {
    LotteryRecordStatus status = switch (order.getStatus()) {
      case SUCCESS -> LotteryRecordStatus.SUCCESS;
      case FAILED -> LotteryRecordStatus.FAILED;
      default -> LotteryRecordStatus.PROCESSING;
    };
    boolean completed = status == LotteryRecordStatus.SUCCESS;
    return new LotteryRecordResponse(
        order.getId(), order.getActivityCode(),
        activity == null ? order.getActivityCode() : activity.getName(), status,
        completed ? order.getPrizeId() : null,
        completed ? order.getPrizeName() : null,
        completed ? order.getPrizeType() : null,
        awardStatus,
        order.getPointsCost(), order.getCreatedAt(), order.getUpdatedAt());
  }

  private LotteryRecordResponse.AwardStatus awardStatus(LotteryAwardProgress progress) {
    return switch (progress.getStatus()) {
      case PENDING -> LotteryRecordResponse.AwardStatus.PENDING;
      case PROCESSING -> LotteryRecordResponse.AwardStatus.PROCESSING;
      case AWARDED -> LotteryRecordResponse.AwardStatus.AWARDED;
      case FAILED -> progress.getRetryCount() < maxAwardRetries
          ? LotteryRecordResponse.AwardStatus.RETRYING : LotteryRecordResponse.AwardStatus.FAILED;
    };
  }
}
