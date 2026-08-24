package com.incentive.award.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.incentive.award.domain.AwardIssuance;
import com.incentive.award.domain.AwardIssuanceStatus;
import com.incentive.award.domain.AwardSourceType;
import com.incentive.award.domain.AwardType;
import com.incentive.award.domain.UserAward;
import com.incentive.award.repository.AwardIssuanceRepository;
import com.incentive.award.repository.UserAwardRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AwardIssuanceStateServiceTest {
  @Mock private AwardIssuanceRepository issuanceRepository;
  @Mock private UserAwardRepository userAwardRepository;
  @Mock private AwardInventoryService inventoryService;
  private AwardIssuanceStateService service;
  private final Instant now = Instant.parse("2026-08-22T10:00:00Z");

  @BeforeEach
  void setUp() {
    service = new AwardIssuanceStateService(
        issuanceRepository, userAwardRepository, inventoryService,
        Clock.fixed(now, ZoneOffset.UTC));
  }

  @Test
  void virtualSuccessCreatesOneUserAwardAndUsesItAsResult() {
    AwardIssuance issuance = processingIssuance(AwardType.VIRTUAL);
    UserAward saved = mock(UserAward.class);
    when(saved.getId()).thenReturn(301L);
    when(issuanceRepository.findByIdForUpdate(81L)).thenReturn(Optional.of(issuance));
    when(userAwardRepository.findByIssuanceId(81L)).thenReturn(Optional.empty());
    when(userAwardRepository.saveAndFlush(any(UserAward.class))).thenReturn(saved);

    service.succeed(81L, null);

    ArgumentCaptor<UserAward> captor = ArgumentCaptor.forClass(UserAward.class);
    verify(userAwardRepository).saveAndFlush(captor.capture());
    verify(inventoryService).consume(issuance);
    assertThat(captor.getValue().getUserId()).isEqualTo(7L);
    assertThat(captor.getValue().getAwardId()).isEqualTo(101L);
    assertThat(captor.getValue().getIssuanceId()).isEqualTo(81L);
    assertThat(captor.getValue().getObtainedAt()).isEqualTo(now);
    verify(issuance).succeed("USER_AWARD:301", now);
  }

  @Test
  void pointsSuccessCreatesUserAwardAndKeepsPointTransactionResult() {
    AwardIssuance issuance = processingIssuance(AwardType.POINTS);
    when(issuanceRepository.findByIdForUpdate(81L)).thenReturn(Optional.of(issuance));
    when(userAwardRepository.findByIssuanceId(81L)).thenReturn(Optional.empty());
    when(userAwardRepository.saveAndFlush(any(UserAward.class)))
        .thenReturn(mock(UserAward.class));

    service.succeed(81L, "POINT_TRANSACTION:71");

    verify(issuance).succeed("POINT_TRANSACTION:71", now);
  }

  private AwardIssuance processingIssuance(AwardType type) {
    AwardIssuance issuance = mock(AwardIssuance.class);
    when(issuance.getId()).thenReturn(81L);
    when(issuance.getStatus()).thenReturn(AwardIssuanceStatus.PROCESSING);
    when(issuance.getUserId()).thenReturn(7L);
    when(issuance.getAwardId()).thenReturn(101L);
    when(issuance.getSourceType()).thenReturn(AwardSourceType.LOTTERY);
    when(issuance.getSourceRecordId()).thenReturn(11L);
    when(issuance.getAwardName()).thenReturn("测试奖品");
    when(issuance.getAwardType()).thenReturn(type);
    when(issuance.getAwardPayload()).thenReturn("{}");
    return issuance;
  }
}
