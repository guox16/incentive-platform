package com.incentive.activity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.incentive.activity.domain.LotteryParticipation;
import com.incentive.activity.domain.PendingAward;
import com.incentive.activity.domain.PrizeType;
import com.incentive.activity.infrastructure.AwardResultMessage;
import com.incentive.activity.repository.PendingAwardRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PendingAwardResultServiceTest {
  private static final Instant NOW = Instant.parse("2026-08-22T00:00:00Z");

  @Test
  void appliesSuccessfulResultOnce() {
    PendingAwardRepository repository = mock(PendingAwardRepository.class);
    PendingAward award = processingAward();
    when(repository.findById(award.getId())).thenReturn(Optional.of(award));
    PendingAwardResultService service = new PendingAwardResultService(
        repository, Clock.fixed(NOW, ZoneOffset.UTC));

    service.apply(new AwardResultMessage(
        award.getId(), "LOTTERY:11", AwardResultMessage.Status.AWARDED,
        "VIRTUAL_AWARD:81", null, null));
    service.apply(new AwardResultMessage(
        award.getId(), "LOTTERY:11", AwardResultMessage.Status.AWARDED,
        "VIRTUAL_AWARD:81", null, null));

    assertThat(award.getStatus()).isEqualTo(PendingAward.Status.AWARDED);
    assertThat(award.getResultRef()).isEqualTo("VIRTUAL_AWARD:81");
  }

  @Test
  void duplicateFailureDoesNotIncreaseRetryCountAgain() {
    PendingAwardRepository repository = mock(PendingAwardRepository.class);
    PendingAward award = processingAward();
    when(repository.findById(award.getId())).thenReturn(Optional.of(award));
    PendingAwardResultService service = new PendingAwardResultService(
        repository, Clock.fixed(NOW, ZoneOffset.UTC));
    AwardResultMessage failure = new AwardResultMessage(
        award.getId(), "LOTTERY:11", AwardResultMessage.Status.FAILED,
        null, "AWARD_ISSUE_FAILED", "失败");

    service.apply(failure);
    service.apply(failure);

    assertThat(award.getRetryCount()).isEqualTo(1);
  }

  private PendingAward processingAward() {
    LotteryParticipation participation = mock(LotteryParticipation.class);
    when(participation.getId()).thenReturn(11L);
    when(participation.getUserId()).thenReturn(7L);
    when(participation.getPrizeId()).thenReturn(101L);
    when(participation.getPrizeName()).thenReturn("优惠券");
    when(participation.getPrizeType()).thenReturn(PrizeType.VIRTUAL);
    PendingAward award = PendingAward.forLottery(participation, NOW.minusSeconds(10));
    ReflectionTestUtils.setField(award, "id", 1L);
    ReflectionTestUtils.setField(award, "status", PendingAward.Status.PROCESSING);
    return award;
  }
}
