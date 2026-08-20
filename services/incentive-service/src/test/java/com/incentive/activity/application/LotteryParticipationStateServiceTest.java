package com.incentive.activity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.incentive.activity.domain.ActivityStatus;
import com.incentive.activity.domain.ActivityType;
import com.incentive.activity.domain.IncentiveActivity;
import com.incentive.activity.domain.LotteryOrder;
import com.incentive.activity.domain.LotteryOrderStatus;
import com.incentive.activity.domain.LotteryParticipation;
import com.incentive.activity.domain.LotteryParticipationStatus;
import com.incentive.activity.domain.LotteryPrize;
import com.incentive.activity.domain.ParticipationRule;
import com.incentive.activity.domain.PendingAward;
import com.incentive.activity.domain.PrizeType;
import com.incentive.activity.repository.LotteryOrderRepository;
import com.incentive.activity.repository.LotteryParticipationRepository;
import com.incentive.activity.repository.PendingAwardRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LotteryParticipationStateServiceTest {
  private static final Instant NOW = Instant.parse("2026-08-11T12:00:00Z");
  @Mock private LotteryOrderRepository orderRepository;
  @Mock private LotteryParticipationRepository participationRepository;
  @Mock private PendingAwardRepository pendingAwardRepository;
  private LotteryParticipationStateService service;

  @BeforeEach
  void setUp() {
    service = new LotteryParticipationStateService(orderRepository, participationRepository,
        pendingAwardRepository, Clock.fixed(NOW, ZoneId.of("Asia/Shanghai")));
  }

  @Test
  void savesWaitingRecordAndMarksOrderResultSavedAtomically() {
    LotteryOrder order = pointsReservedOrder(PrizeType.VIRTUAL);
    when(orderRepository.findByIdForUpdate(7001L)).thenReturn(Optional.of(order));
    when(participationRepository.findByLotteryOrderId(7001L)).thenReturn(Optional.empty());
    when(participationRepository.saveAndFlush(any(LotteryParticipation.class)))
        .thenAnswer(invocation -> {
          LotteryParticipation participation = invocation.getArgument(0);
          ReflectionTestUtils.setField(participation, "id", 51L);
          return participation;
        });

    LotteryParticipation participation = service.saveWaiting(7001L);

    assertThat(participation.getLotteryOrderId()).isEqualTo(7001L);
    assertThat(participation.getStatus())
        .isEqualTo(LotteryParticipationStatus.WAITING_CONFIRMATION);
    assertThat(participation.getPointTransactionId()).isNull();
    assertThat(order.getStatus()).isEqualTo(LotteryOrderStatus.RESULT_SAVED);
  }

  @Test
  void completesRecordOrderAndPendingAwardTogether() {
    LotteryOrder order = pointsReservedOrder(PrizeType.VIRTUAL);
    order.markResultSaved(NOW);
    LotteryParticipation participation = new LotteryParticipation(order, NOW);
    ReflectionTestUtils.setField(participation, "id", 51L);
    when(orderRepository.findByIdForUpdate(7001L)).thenReturn(Optional.of(order));
    when(participationRepository.findByLotteryOrderIdForUpdate(7001L))
        .thenReturn(Optional.of(participation));

    var result = service.complete(7001L, 44L);

    assertThat(result.participation().getStatus()).isEqualTo(LotteryParticipationStatus.SUCCESS);
    assertThat(result.participation().getPointTransactionId()).isEqualTo(44L);
    assertThat(order.getStatus()).isEqualTo(LotteryOrderStatus.SUCCESS);
    assertThat(result.pendingAwardCreated()).isTrue();
    ArgumentCaptor<PendingAward> awardCaptor = ArgumentCaptor.forClass(PendingAward.class);
    verify(pendingAwardRepository).save(awardCaptor.capture());
    PendingAward award = awardCaptor.getValue();
    assertThat(award.getSourceType()).isEqualTo(PendingAward.SourceType.LOTTERY);
    assertThat(award.getSourceRecordId()).isEqualTo(51L);
    assertThat(award.getUserId()).isEqualTo(7L);
    assertThat(award.getPrizeId()).isEqualTo(131L);
    assertThat(award.getPrizeType()).isEqualTo(PrizeType.VIRTUAL);
    assertThat(award.getStatus()).isEqualTo(PendingAward.Status.PENDING);
    assertThat(award.getRetryCount()).isZero();
  }

  @Test
  void nonePrizeCompletesWithoutPendingAward() {
    LotteryOrder order = pointsReservedOrder(PrizeType.NONE);
    order.markResultSaved(NOW);
    LotteryParticipation participation = new LotteryParticipation(order, NOW);
    ReflectionTestUtils.setField(participation, "id", 51L);
    when(orderRepository.findByIdForUpdate(7001L)).thenReturn(Optional.of(order));
    when(participationRepository.findByLotteryOrderIdForUpdate(7001L))
        .thenReturn(Optional.of(participation));

    var result = service.complete(7001L, 44L);

    assertThat(result.pendingAwardCreated()).isFalse();
    verify(pendingAwardRepository, never()).save(any());
  }

  @Test
  void repeatedSaveReturnsExistingRecordWithoutCreatingAnotherOne() {
    LotteryOrder order = pointsReservedOrder(PrizeType.VIRTUAL);
    order.markResultSaved(NOW);
    LotteryParticipation participation = new LotteryParticipation(order, NOW);
    ReflectionTestUtils.setField(participation, "id", 51L);
    when(orderRepository.findByIdForUpdate(7001L)).thenReturn(Optional.of(order));
    when(participationRepository.findByLotteryOrderId(7001L))
        .thenReturn(Optional.of(participation));

    LotteryParticipation replay = service.saveWaiting(7001L);

    assertThat(replay).isSameAs(participation);
    verify(participationRepository, never()).saveAndFlush(any());
  }

  @Test
  void repeatedCompletionKeepsOriginalResultWithoutCreatingAnotherAward() {
    LotteryOrder order = pointsReservedOrder(PrizeType.VIRTUAL);
    order.markResultSaved(NOW);
    LotteryParticipation participation = new LotteryParticipation(order, NOW);
    ReflectionTestUtils.setField(participation, "id", 51L);
    participation.markSuccess(44L, NOW);
    order.markSuccess(NOW);
    when(orderRepository.findByIdForUpdate(7001L)).thenReturn(Optional.of(order));
    when(participationRepository.findByLotteryOrderIdForUpdate(7001L))
        .thenReturn(Optional.of(participation));

    var replay = service.complete(7001L, 44L);

    assertThat(replay.participation()).isSameAs(participation);
    assertThat(replay.pendingAwardCreated()).isTrue();
    verify(pendingAwardRepository, never()).save(any());
  }

  private LotteryOrder pointsReservedOrder(PrizeType type) {
    IncentiveActivity activity = BeanUtils.instantiateClass(IncentiveActivity.class);
    ReflectionTestUtils.setField(activity, "id", 1L);
    ReflectionTestUtils.setField(activity, "code", "SUMMER_LOTTERY");
    ReflectionTestUtils.setField(activity, "type", ActivityType.LOTTERY);
    ReflectionTestUtils.setField(activity, "status", ActivityStatus.ACTIVE);
    ParticipationRule rule = BeanUtils.instantiateClass(ParticipationRule.class);
    ReflectionTestUtils.setField(rule, "id", 2L);
    ReflectionTestUtils.setField(rule, "ruleVersion", 1);
    ReflectionTestUtils.setField(rule, "pointsCost", 10L);
    LotteryPrize prize = BeanUtils.instantiateClass(LotteryPrize.class);
    ReflectionTestUtils.setField(prize, "id", 31L);
    ReflectionTestUtils.setField(prize, "prizeId", 131L);
    ReflectionTestUtils.setField(prize, "prizeName", type == PrizeType.NONE ? "谢谢参与" : "优惠券");
    ReflectionTestUtils.setField(prize, "prizeType", type);
    LotteryOrder order = new LotteryOrder(7001L, "request-1", 7L, activity, rule, prize,
        9001L, "{\"passed\":true,\"usedTodayBefore\":0}", NOW);
    order.markPointsReserved(NOW.plusSeconds(300), NOW);
    return order;
  }
}
