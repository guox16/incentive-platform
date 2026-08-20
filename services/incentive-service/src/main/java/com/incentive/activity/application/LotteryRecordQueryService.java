package com.incentive.activity.application;

import com.incentive.activity.domain.IncentiveActivity;
import com.incentive.activity.domain.LotteryOrder;
import com.incentive.activity.domain.LotteryOrderStatus;
import com.incentive.activity.dto.LotteryRecordResponse;
import com.incentive.activity.dto.LotteryRecordStatus;
import com.incentive.activity.repository.IncentiveActivityRepository;
import com.incentive.activity.repository.LotteryOrderRepository;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LotteryRecordQueryService {
  private static final int RECENT_RECORD_LIMIT = 20;

  private final LotteryOrderRepository orderRepository;
  private final IncentiveActivityRepository activityRepository;

  public LotteryRecordQueryService(LotteryOrderRepository orderRepository,
      IncentiveActivityRepository activityRepository) {
    this.orderRepository = orderRepository;
    this.activityRepository = activityRepository;
  }

  /** 抽奖单覆盖尚未生成参与记录的中间态，因此记录查询以抽奖单为准。 */
  @Transactional(readOnly = true)
  public List<LotteryRecordResponse> findRecentByUser(Long userId) {
    List<LotteryOrder> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(
        userId, PageRequest.of(0, RECENT_RECORD_LIMIT));
    Map<Long, IncentiveActivity> activities = activityRepository.findAllById(
            orders.stream().map(LotteryOrder::getActivityId).distinct().toList())
        .stream().collect(Collectors.toMap(IncentiveActivity::getId, Function.identity()));
    return orders.stream().map(order -> toResponse(order, activities.get(order.getActivityId())))
        .toList();
  }

  private LotteryRecordResponse toResponse(
      LotteryOrder order, IncentiveActivity activity) {
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
        order.getPointsCost(), order.getCreatedAt(), order.getUpdatedAt());
  }
}
