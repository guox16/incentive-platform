package com.incentive.activity.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.incentive.activity.domain.LotteryParticipation;
import com.incentive.activity.domain.PendingAward;
import com.incentive.activity.domain.PrizeType;
import jakarta.persistence.EntityManager;
import java.time.Instant;
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
