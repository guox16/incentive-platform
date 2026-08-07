package com.incentive.activity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.incentive.activity.domain.DailyCheckIn;
import com.incentive.activity.infrastructure.PointsClient;
import com.incentive.activity.repository.DailyCheckInRepository;
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

  @Mock private DailyCheckInRepository repository;
  @Mock private PointsClient pointsClient;
  private DailyCheckInService service;

  @BeforeEach
  void setUp() {
    Clock clock = Clock.fixed(NOW, ZoneId.of("Asia/Shanghai"));
    service = new DailyCheckInService(repository, pointsClient, clock);
  }

  @Test
  void firstCheckInAwardsTenPointsAndStartsStreak() {
    when(repository.findByUserIdAndCheckInDate(USER_ID, TODAY)).thenReturn(Optional.empty());
    when(repository.findTopByUserIdOrderByCheckInDateDesc(USER_ID)).thenReturn(Optional.empty());
    when(repository.saveAndFlush(any(DailyCheckIn.class))).thenAnswer(invocation -> {
      DailyCheckIn record = invocation.getArgument(0);
      ReflectionTestUtils.setField(record, "id", 21L);
      return record;
    });
    when(pointsClient.credit(21L, USER_ID, 10)).thenReturn(new PointsClient.PointCreditResult(31L, 110));
    when(repository.findByUserIdAndCheckInDateBetweenOrderByCheckInDateAsc(USER_ID,
        TODAY.withDayOfMonth(1), TODAY.withDayOfMonth(31))).thenAnswer(invocation -> List.of());

    var response = service.checkIn(USER_ID);

    assertThat(response.checkedInToday()).isTrue();
    assertThat(response.currentStreak()).isEqualTo(1);
    assertThat(response.rewardPoints()).isEqualTo(10);
    assertThat(response.rewardStatus()).isEqualTo("AWARDED");
    assertThat(response.balanceAfter()).isEqualTo(110);
  }

  @Test
  void consecutiveCheckInContinuesPreviousStreak() {
    DailyCheckIn yesterday = record(TODAY.minusDays(1), 2, 20L, true);
    when(repository.findByUserIdAndCheckInDate(USER_ID, TODAY)).thenReturn(Optional.empty());
    when(repository.findTopByUserIdOrderByCheckInDateDesc(USER_ID)).thenReturn(Optional.of(yesterday));
    when(repository.saveAndFlush(any(DailyCheckIn.class))).thenAnswer(invocation -> {
      DailyCheckIn record = invocation.getArgument(0);
      ReflectionTestUtils.setField(record, "id", 22L);
      return record;
    });
    when(pointsClient.credit(22L, USER_ID, 10)).thenReturn(new PointsClient.PointCreditResult(32L, 120));
    when(repository.findByUserIdAndCheckInDateBetweenOrderByCheckInDateAsc(any(), any(), any()))
        .thenReturn(List.of(yesterday));

    var response = service.checkIn(USER_ID);

    assertThat(response.currentStreak()).isEqualTo(3);
  }

  @Test
  void repeatedCheckInReturnsExistingAwardWithoutCreditingAgain() {
    DailyCheckIn existing = record(TODAY, 3, 23L, true);
    when(repository.findByUserIdAndCheckInDate(USER_ID, TODAY)).thenReturn(Optional.of(existing));
    when(repository.findByUserIdAndCheckInDateBetweenOrderByCheckInDateAsc(any(), any(), any()))
        .thenReturn(List.of(existing));

    var response = service.checkIn(USER_ID);

    assertThat(response.checkedInToday()).isTrue();
    assertThat(response.currentStreak()).isEqualTo(3);
    assertThat(response.signedDates()).containsExactly(TODAY);
    verify(pointsClient, never()).credit(any(), any(), anyLong());
  }

  @Test
  void missedDayResetsDisplayedStreakUntilNextCheckIn() {
    DailyCheckIn older = record(TODAY.minusDays(2), 4, 19L, true);
    when(repository.findByUserIdAndCheckInDate(USER_ID, TODAY)).thenReturn(Optional.empty());
    when(repository.findTopByUserIdOrderByCheckInDateDesc(USER_ID)).thenReturn(Optional.of(older));
    when(repository.findByUserIdAndCheckInDateBetweenOrderByCheckInDateAsc(any(), any(), any()))
        .thenReturn(List.of(older));

    var response = service.getStatus(USER_ID);

    assertThat(response.checkedInToday()).isFalse();
    assertThat(response.currentStreak()).isZero();
    assertThat(response.rewardStatus()).isEqualTo("AVAILABLE");
  }

  private DailyCheckIn record(LocalDate date, int streak, long id, boolean awarded) {
    DailyCheckIn record = new DailyCheckIn(USER_ID, date, streak, 10, NOW);
    ReflectionTestUtils.setField(record, "id", id);
    if (awarded) record.markAwarded(id + 100, NOW);
    return record;
  }
}
