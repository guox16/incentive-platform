package com.incentive.activity.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.incentive.activity.domain.LotteryParticipation;
import com.incentive.activity.domain.LotteryOrder;
import com.incentive.activity.domain.PendingAward;
import com.incentive.activity.domain.PrizeType;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
class PendingAwardRepositoryTest {
  private static final Instant NOW = Instant.parse("2026-08-22T00:00:00Z");

  @Autowired private PendingAwardRepository repository;
  @Autowired private EntityManager entityManager;

  @Test
  void findsAndAtomicallyClaimsPendingAward() {
    PendingAward award = repository.saveAndFlush(PendingAward.forLottery(participation(), NOW));

    assertThat(repository.findDispatchCandidateIds(
        NOW.minusSeconds(30), NOW.minusSeconds(30), 8, 0, 1, PageRequest.of(0, 10)))
        .containsExactly(award.getId());
    assertThat(repository.claimForDispatch(
        award.getId(), NOW.plusSeconds(1), NOW.minusSeconds(30),
        NOW.minusSeconds(30), 8)).isEqualTo(1);

    entityManager.clear();
    assertThat(repository.findById(award.getId()).orElseThrow().getStatus())
        .isEqualTo(PendingAward.Status.PROCESSING);
  }

  @Test
  void joinsAwardProgressToTheOwnersLotteryOrder() {
    LotteryOrder order = mock(LotteryOrder.class);
    when(order.getId()).thenReturn(420L);
    when(order.getActivityId()).thenReturn(1L);
    when(order.getRuleId()).thenReturn(2L);
    when(order.getUserId()).thenReturn(7L);
    when(order.getLotteryPrizeId()).thenReturn(3L);
    when(order.getPrizeId()).thenReturn(101L);
    when(order.getPrizeName()).thenReturn("优惠券");
    when(order.getPrizeType()).thenReturn(PrizeType.VIRTUAL);
    LotteryParticipation participation = new LotteryParticipation(order, NOW);
    entityManager.persist(participation);
    PendingAward award = PendingAward.forLottery(participation, NOW);
    award.markAwarded("USER_AWARD:10", NOW);
    repository.saveAndFlush(award);

    var progress = repository.findLotteryAwardProgress(7L, PendingAward.SourceType.LOTTERY,
        List.of(420L));
    assertThat(progress).singleElement().satisfies(result -> {
      assertThat(result.getOrderId()).isEqualTo(420L);
      assertThat(result.getStatus()).isEqualTo(PendingAward.Status.AWARDED);
    });
    assertThat(repository.findLotteryAwardProgress(8L, PendingAward.SourceType.LOTTERY,
        List.of(420L))).isEmpty();
  }

  private LotteryParticipation participation() {
    LotteryParticipation participation = mock(LotteryParticipation.class);
    when(participation.getId()).thenReturn(11L);
    when(participation.getUserId()).thenReturn(7L);
    when(participation.getPrizeId()).thenReturn(101L);
    when(participation.getPrizeName()).thenReturn("优惠券");
    when(participation.getPrizeType()).thenReturn(PrizeType.VIRTUAL);
    when(participation.getAwardPayload()).thenReturn("{\"templateCode\":\"WELCOME\"}");
    when(participation.getStockNo()).thenReturn(3L);
    return participation;
  }
}
