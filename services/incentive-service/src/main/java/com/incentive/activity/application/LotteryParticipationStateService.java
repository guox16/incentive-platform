package com.incentive.activity.application;

import com.incentive.activity.domain.LotteryOrder;
import com.incentive.activity.domain.LotteryOrderStatus;
import com.incentive.activity.domain.LotteryParticipation;
import com.incentive.activity.domain.LotteryParticipationStatus;
import com.incentive.activity.domain.PendingAward;
import com.incentive.activity.domain.PrizeType;
import com.incentive.activity.repository.LotteryOrderRepository;
import com.incentive.activity.repository.LotteryParticipationRepository;
import com.incentive.activity.repository.PendingAwardRepository;
import com.incentive.activity.support.IncentiveBusinessException;
import java.time.Clock;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LotteryParticipationStateService {
  private final LotteryOrderRepository orderRepository;
  private final LotteryParticipationRepository participationRepository;
  private final PendingAwardRepository pendingAwardRepository;
  private final Clock clock;

  public LotteryParticipationStateService(LotteryOrderRepository orderRepository,
      LotteryParticipationRepository participationRepository,
      PendingAwardRepository pendingAwardRepository, Clock clock) {
    this.orderRepository = orderRepository;
    this.participationRepository = participationRepository;
    this.pendingAwardRepository = pendingAwardRepository;
    this.clock = clock;
  }

  /** 原子写入待确认抽奖记录，并把抽奖单推进到 RESULT_SAVED。 */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public LotteryParticipation saveWaiting(Long orderId) {
    LotteryOrder order = loadOrderForUpdate(orderId);
    LotteryParticipation existing = participationRepository.findByLotteryOrderId(orderId)
        .orElse(null);
    if (existing != null) {
      if (order.getStatus() == LotteryOrderStatus.POINTS_RESERVED) {
        order.markResultSaved(clock.instant());
      } else if (order.getStatus() != LotteryOrderStatus.RESULT_SAVED
          && order.getStatus() != LotteryOrderStatus.SUCCESS) {
        throw invalidState("抽奖单状态与已存在的抽奖记录不一致");
      }
      return existing;
    }

    Instant now = clock.instant();
    order.markResultSaved(now);
    return participationRepository.saveAndFlush(new LotteryParticipation(order, now));
  }

  /** 积分确认后原子完成记录、创建待发奖任务并完成抽奖单。 */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public CompletionResult complete(Long orderId, Long pointTransactionId) {
    LotteryOrder order = loadOrderForUpdate(orderId);
    LotteryParticipation participation = participationRepository
        .findByLotteryOrderIdForUpdate(orderId)
        .orElseThrow(() -> invalidState("抽奖记录不存在，不能完成抽奖"));
    Instant now = clock.instant();

    if (order.getStatus() == LotteryOrderStatus.SUCCESS
        && participation.getStatus() == LotteryParticipationStatus.SUCCESS) {
      participation.markSuccess(pointTransactionId, now);
      return new CompletionResult(participation, order.getPrizeType() != PrizeType.NONE);
    }
    if (order.getStatus() != LotteryOrderStatus.RESULT_SAVED
        || participation.getStatus() != LotteryParticipationStatus.WAITING_CONFIRMATION) {
      throw invalidState("抽奖单或抽奖记录状态不允许完成");
    }

    participation.markSuccess(pointTransactionId, now);
    boolean pendingAwardCreated = order.getPrizeType() != PrizeType.NONE;
    if (pendingAwardCreated) {
      pendingAwardRepository.save(PendingAward.forLottery(participation, now));
    }
    order.markSuccess(now);
    return new CompletionResult(participation, pendingAwardCreated);
  }

  private LotteryOrder loadOrderForUpdate(Long orderId) {
    return orderRepository.findByIdForUpdate(orderId)
        .orElseThrow(() -> new IncentiveBusinessException(
            "LOTTERY_ORDER_NOT_FOUND", "抽奖单不存在", HttpStatus.NOT_FOUND));
  }

  private IncentiveBusinessException invalidState(String message) {
    return new IncentiveBusinessException(
        "LOTTERY_ORDER_INVALID_STATE", message, HttpStatus.CONFLICT);
  }

  public record CompletionResult(
      LotteryParticipation participation, boolean pendingAwardCreated) {}
}
