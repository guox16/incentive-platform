package com.incentive.activity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.incentive.activity.domain.DailyCheckIn;
import com.incentive.activity.infrastructure.PointsClient;
import com.incentive.activity.repository.DailyCheckInRepository;
import com.incentive.activity.support.IncentiveBusinessException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DailyCheckInServiceTest {
  private static final Instant NOW = Instant.parse("2026-08-07T03:00:00Z");
  private static final LocalDate TODAY = LocalDate.of(2026, 8, 7);
  private static final Long USER_ID = 1L;
  private static final String REQUEST_ID = "check-in-request-1";

  @Mock private DailyCheckInRepository repository;
  @Mock private DailyCheckInTransactions transactions;
  @Mock private PointsClient pointsClient;
  private DailyCheckInService service;

  @BeforeEach
  void setUp() {
    Clock clock = Clock.fixed(NOW, ZoneId.of("Asia/Shanghai"));
    service = new DailyCheckInService(repository, transactions, pointsClient, clock);
  }

  @Test
  void firstCheckInAwardsTenPointsAndStartsStreak() {
    when(repository.findByRequestId(REQUEST_ID)).thenReturn(Optional.empty());
    when(repository.findByUserIdAndCheckInDate(USER_ID, TODAY)).thenReturn(Optional.empty());
    when(repository.findTopByUserIdOrderByCheckInDateDesc(USER_ID)).thenReturn(Optional.empty());
    DailyCheckIn created = record(REQUEST_ID, TODAY, 1, 21L, false);
    when(transactions.create(REQUEST_ID, USER_ID, TODAY, 1, 10, NOW)).thenReturn(created);
    when(pointsClient.credit(21L, USER_ID, 10)).thenReturn(new PointsClient.PointCreditResult(31L, 110));
    when(transactions.markAwarded(21L, 31L, NOW)).thenAnswer(invocation -> {
      created.markAwarded(31L, NOW);
      return created;
    });
    when(repository.findByUserIdAndCheckInDateBetweenOrderByCheckInDateAsc(USER_ID,
        TODAY.withDayOfMonth(1), TODAY.withDayOfMonth(31))).thenAnswer(invocation -> List.of());

    var response = service.checkIn(USER_ID, REQUEST_ID);

    assertThat(response.checkedInToday()).isTrue();
    assertThat(response.currentStreak()).isEqualTo(1);
    assertThat(response.rewardPoints()).isEqualTo(10);
    assertThat(response.rewardStatus()).isEqualTo("AWARDED");
    assertThat(response.balanceAfter()).isEqualTo(110);
  }

  @Test
  void consecutiveCheckInContinuesPreviousStreak() {
    DailyCheckIn yesterday = record("yesterday", TODAY.minusDays(1), 2, 20L, true);
    when(repository.findByRequestId(REQUEST_ID)).thenReturn(Optional.empty());
    when(repository.findByUserIdAndCheckInDate(USER_ID, TODAY)).thenReturn(Optional.empty());
    when(repository.findTopByUserIdOrderByCheckInDateDesc(USER_ID)).thenReturn(Optional.of(yesterday));
    DailyCheckIn created = record(REQUEST_ID, TODAY, 3, 22L, false);
    when(transactions.create(REQUEST_ID, USER_ID, TODAY, 3, 10, NOW)).thenReturn(created);
    when(pointsClient.credit(22L, USER_ID, 10)).thenReturn(new PointsClient.PointCreditResult(32L, 120));
    when(transactions.markAwarded(22L, 32L, NOW)).thenAnswer(invocation -> {
      created.markAwarded(32L, NOW);
      return created;
    });
    when(repository.findByUserIdAndCheckInDateBetweenOrderByCheckInDateAsc(any(), any(), any()))
        .thenReturn(List.of(yesterday));

    var response = service.checkIn(USER_ID, REQUEST_ID);

    assertThat(response.currentStreak()).isEqualTo(3);
  }

  @Test
  void repeatedCheckInReturnsExistingAwardWithoutCreditingAgain() {
    DailyCheckIn existing = record(REQUEST_ID, TODAY, 3, 23L, true);
    when(repository.findByRequestId(REQUEST_ID)).thenReturn(Optional.of(existing));
    when(repository.findByUserIdAndCheckInDateBetweenOrderByCheckInDateAsc(any(), any(), any()))
        .thenReturn(List.of(existing));

    var response = service.checkIn(USER_ID, REQUEST_ID);

    assertThat(response.checkedInToday()).isTrue();
    assertThat(response.currentStreak()).isEqualTo(3);
    assertThat(response.signedDates()).containsExactly(TODAY);
    verify(pointsClient, never()).credit(any(), any(), anyLong());
  }

  @Test
  void missedDayResetsDisplayedStreakUntilNextCheckIn() {
    DailyCheckIn older = record("older", TODAY.minusDays(2), 4, 19L, true);
    when(repository.findByUserIdAndCheckInDate(USER_ID, TODAY)).thenReturn(Optional.empty());
    when(repository.findTopByUserIdOrderByCheckInDateDesc(USER_ID)).thenReturn(Optional.of(older));
    when(repository.findByUserIdAndCheckInDateBetweenOrderByCheckInDateAsc(any(), any(), any()))
        .thenReturn(List.of(older));

    var response = service.getStatus(USER_ID);

    assertThat(response.checkedInToday()).isFalse();
    assertThat(response.currentStreak()).isZero();
    assertThat(response.rewardStatus()).isEqualTo("AVAILABLE");
  }

  @Test
  void leavesPersistedCheckInPendingWhenPointsServiceFails() {
    when(repository.findByRequestId(REQUEST_ID)).thenReturn(Optional.empty());
    when(repository.findByUserIdAndCheckInDate(USER_ID, TODAY)).thenReturn(Optional.empty());
    when(repository.findTopByUserIdOrderByCheckInDateDesc(USER_ID)).thenReturn(Optional.empty());
    DailyCheckIn created = record(REQUEST_ID, TODAY, 1, 24L, false);
    when(transactions.create(REQUEST_ID, USER_ID, TODAY, 1, 10, NOW)).thenReturn(created);
    when(pointsClient.credit(24L, USER_ID, 10)).thenThrow(new IncentiveBusinessException(
        "CHECK_IN_REWARD_PENDING", "签到已记录，积分发放暂未完成，请重试",
        org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE));

    assertThatThrownBy(() -> service.checkIn(USER_ID, REQUEST_ID))
        .isInstanceOf(IncentiveBusinessException.class)
        .hasMessage("签到已记录，积分发放暂未完成，请重试");

    assertThat(created.getRewardStatus().name()).isEqualTo("PENDING");
    verify(transactions, never()).markAwarded(any(), any(), any());
  }

  @Test
  void retriesPendingRewardWithSamePersistedCheckInId() {
    DailyCheckIn pending = record(REQUEST_ID, TODAY, 2, 25L, false);
    when(repository.findByRequestId(REQUEST_ID)).thenReturn(Optional.of(pending));
    when(pointsClient.credit(25L, USER_ID, 10)).thenReturn(new PointsClient.PointCreditResult(35L, 130));
    when(transactions.markAwarded(25L, 35L, NOW)).thenAnswer(invocation -> {
      pending.markAwarded(35L, NOW);
      return pending;
    });
    when(repository.findByUserIdAndCheckInDateBetweenOrderByCheckInDateAsc(any(), any(), any()))
        .thenReturn(List.of(pending));

    var response = service.checkIn(USER_ID, REQUEST_ID);

    assertThat(response.rewardStatus()).isEqualTo("AWARDED");
    assertThat(response.checkInId()).isEqualTo(25L);
    verify(transactions, never()).create(any(), any(), any(), any(Integer.class), anyLong(), any());
  }

  private DailyCheckIn record(String requestId, LocalDate date, int streak, long id, boolean awarded) {
    DailyCheckIn record = new DailyCheckIn(requestId, USER_ID, date, streak, 10, NOW);
    ReflectionTestUtils.setField(record, "id", id);
    if (awarded) record.markAwarded(id + 100, NOW);
    return record;
  }
}
