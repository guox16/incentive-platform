package com.incentive.award.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incentive.award.domain.AwardIssuance;
import com.incentive.award.domain.AwardIssuanceStatus;
import com.incentive.award.domain.AwardSourceType;
import com.incentive.award.domain.AwardType;
import com.incentive.award.infrastructure.AwardDeliveryException;
import com.incentive.award.infrastructure.AwardPointsClient;
import com.incentive.award.messaging.AwardCommandMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AwardIssuanceServiceTest {
  @Mock private AwardIssuanceCoordinator coordinator;
  @Mock private AwardIssuanceStateService stateService;
  @Mock private AwardPointsClient pointsClient;
  private AwardIssuanceService service;

  @BeforeEach
  void setUp() {
    service = new AwardIssuanceService(
        coordinator, stateService, pointsClient, new ObjectMapper());
  }

  @Test
  void duplicateSuccessfulCommandReturnsOriginalResultWithoutIssuingAgain() {
    AwardCommandMessage command = command(AwardType.POINTS, "{\"points\":100}");
    AwardIssuance issuance = issuance(AwardIssuanceStatus.SUCCEEDED);
    when(issuance.getResultRef()).thenReturn("POINT_TRANSACTION:71");
    when(coordinator.prepare(command)).thenReturn(issuance);

    var result = service.issue(command);

    assertThat(result.replayed()).isTrue();
    assertThat(result.resultRef()).isEqualTo("POINT_TRANSACTION:71");
    verify(pointsClient, never()).credit(9001L, 7L, 100L, "100积分");
  }

  @Test
  void pointsCommandUsesStableBusinessId() {
    AwardCommandMessage command = command(AwardType.POINTS, "{\"points\":100}");
    AwardIssuance processing = issuance(AwardIssuanceStatus.PROCESSING);
    stubPointsIssuance(processing, "{\"points\":100}");
    AwardIssuance succeeded = mock(AwardIssuance.class);
    when(succeeded.getId()).thenReturn(81L);
    when(succeeded.getResultRef()).thenReturn("POINT_TRANSACTION:71");
    when(coordinator.prepare(command)).thenReturn(processing);
    when(pointsClient.credit(9001L, 7L, 100L, "100积分"))
        .thenReturn(new AwardPointsClient.PointCreditResult(71L, 500L));
    when(stateService.succeed(81L, "POINT_TRANSACTION:71")).thenReturn(succeeded);

    var result = service.issue(command);

    assertThat(result.replayed()).isFalse();
    assertThat(result.resultRef()).isEqualTo("POINT_TRANSACTION:71");
    verify(pointsClient).credit(9001L, 7L, 100L, "100积分");
  }

  @Test
  void invalidPayloadIsRecordedAsFailure() {
    AwardCommandMessage command = command(AwardType.POINTS, "{}");
    AwardIssuance processing = issuance(AwardIssuanceStatus.PROCESSING);
    when(processing.getAwardType()).thenReturn(AwardType.POINTS);
    when(processing.getAwardPayload()).thenReturn("{}");
    when(coordinator.prepare(command)).thenReturn(processing);

    assertThatThrownBy(() -> service.issue(command))
        .isInstanceOf(AwardDeliveryException.class)
        .hasMessage("积分奖品必须配置正整数points");
    verify(stateService).fail(81L, "AWARD_PAYLOAD_INVALID", "积分奖品必须配置正整数points");
  }

  @Test
  void virtualAwardOnlyCreatesUserAwardRecord() {
    AwardCommandMessage command = command(AwardType.VIRTUAL, "{}");
    AwardIssuance processing = issuance(AwardIssuanceStatus.PROCESSING);
    AwardIssuance succeeded = mock(AwardIssuance.class);
    when(processing.getAwardType()).thenReturn(AwardType.VIRTUAL);
    when(succeeded.getId()).thenReturn(81L);
    when(succeeded.getResultRef()).thenReturn("USER_AWARD:301");
    when(coordinator.prepare(command)).thenReturn(processing);
    when(stateService.succeed(81L, null)).thenReturn(succeeded);

    var result = service.issue(command);

    assertThat(result.resultRef()).isEqualTo("USER_AWARD:301");
    verify(stateService).succeed(81L, null);
    verify(pointsClient, never()).credit(9001L, 7L, 100L, "100积分");
  }

  private AwardIssuance issuance(AwardIssuanceStatus status) {
    AwardIssuance issuance = mock(AwardIssuance.class);
    when(issuance.getId()).thenReturn(81L);
    when(issuance.getStatus()).thenReturn(status);
    return issuance;
  }

  private void stubPointsIssuance(AwardIssuance issuance, String payload) {
    when(issuance.getAwardType()).thenReturn(AwardType.POINTS);
    when(issuance.getAwardPayload()).thenReturn(payload);
    when(issuance.getPointBusinessId()).thenReturn(9001L);
    when(issuance.getUserId()).thenReturn(7L);
    when(issuance.getAwardName()).thenReturn("100积分");
  }

  private AwardCommandMessage command(AwardType type, String payload) {
    return new AwardCommandMessage(
        1L, "LOTTERY:11", AwardSourceType.LOTTERY, 11L,
        7L, 101L, "100积分", type, payload, 3L);
  }
}
