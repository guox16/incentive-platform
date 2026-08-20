package com.incentive.activity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.incentive.activity.domain.ActivityType;
import com.incentive.activity.domain.IncentiveActivity;
import com.incentive.activity.domain.ParticipationRule;
import com.incentive.activity.domain.PrizeType;
import com.incentive.activity.domain.RedemptionItem;
import com.incentive.activity.domain.RedemptionRecord;
import com.incentive.activity.dto.RedemptionResponse;
import com.incentive.activity.infrastructure.BusinessNumberGenerator;
import com.incentive.activity.infrastructure.PointsClient;
import com.incentive.activity.repository.RedemptionRecordRepository;
import com.incentive.activity.support.IncentiveBusinessException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RedemptionServiceTest {
  private static final Instant NOW = Instant.parse("2026-08-11T12:00:00Z");
  @Mock private RedemptionRecordRepository recordRepository;
  @Mock private RedemptionTransactions transactions;
  @Mock private PointsClient pointsClient;
  @Mock private BusinessNumberGenerator businessNumberGenerator;
  private RedemptionService service;

  @BeforeEach
  void setUp() {
    service = new RedemptionService(
        recordRepository, transactions, pointsClient, businessNumberGenerator);
  }

  @Test
  void newRequestPersistsBeforeDebitAndCompletesAwardPreparation() {
    RedemptionRecord record = pendingRecord("request-1", 8001L);
    when(recordRepository.findByRequestId("request-1")).thenReturn(Optional.empty());
    when(businessNumberGenerator.next()).thenReturn(8001L);
    when(transactions.createPending("request-1", "POINTS_MALL", 10L, 7L, 8001L))
        .thenReturn(record);
    when(pointsClient.debit(8001L, 7L, 50L, "REDEMPTION", "兑换商品：WELCOME_COUPON"))
        .thenReturn(new PointsClient.PointDebitResult(61L, 150L));
    when(transactions.complete(71L, 61L, 150L)).thenAnswer(invocation -> {
      record.complete(61L, 150L, NOW);
      return record;
    });

    RedemptionResponse response = service.redeem("POINTS_MALL", 10L, 7L, "request-1");

    assertThat(response.redemptionId()).isEqualTo(71L);
    assertThat(response.balanceAfter()).isEqualTo(150L);
    verify(transactions).createPending("request-1", "POINTS_MALL", 10L, 7L, 8001L);
    verify(pointsClient).debit(8001L, 7L, 50L, "REDEMPTION", "兑换商品：WELCOME_COUPON");
  }

  @Test
  void retryAfterUnknownDebitResultReusesPersistedPointBusinessId() {
    RedemptionRecord record = pendingRecord("request-2", 8002L);
    when(recordRepository.findByRequestId("request-2"))
        .thenReturn(Optional.empty(), Optional.of(record));
    when(businessNumberGenerator.next()).thenReturn(8002L);
    when(transactions.createPending("request-2", "POINTS_MALL", 10L, 7L, 8002L))
        .thenReturn(record);
    when(pointsClient.debit(8002L, 7L, 50L, "REDEMPTION", "兑换商品：WELCOME_COUPON"))
        .thenThrow(new IncentiveBusinessException(
            "POINTS_SERVICE_UNAVAILABLE", "积分服务暂不可用", HttpStatus.BAD_GATEWAY))
        .thenReturn(new PointsClient.PointDebitResult(62L, 150L));
    when(transactions.complete(71L, 62L, 150L)).thenAnswer(invocation -> {
      record.complete(62L, 150L, NOW);
      return record;
    });

    assertThatThrownBy(() -> service.redeem("POINTS_MALL", 10L, 7L, "request-2"))
        .isInstanceOf(IncentiveBusinessException.class);
    RedemptionResponse response = service.redeem("POINTS_MALL", 10L, 7L, "request-2");

    assertThat(response.pointTransactionId()).isEqualTo(62L);
    verify(transactions).createPending("request-2", "POINTS_MALL", 10L, 7L, 8002L);
    verify(pointsClient, org.mockito.Mockito.times(2))
        .debit(8002L, 7L, 50L, "REDEMPTION", "兑换商品：WELCOME_COUPON");
  }

  @Test
  void completedRequestReturnsOriginalResultWithoutAnotherDebit() {
    RedemptionRecord record = pendingRecord("request-3", 8003L);
    record.complete(63L, 150L, NOW);
    when(recordRepository.findByRequestId("request-3")).thenReturn(Optional.of(record));

    RedemptionResponse response = service.redeem("POINTS_MALL", 10L, 7L, "request-3");

    assertThat(response.pointTransactionId()).isEqualTo(63L);
    assertThat(response.balanceAfter()).isEqualTo(150L);
    verify(pointsClient, never()).debit(org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyLong(),
        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
  }

  @Test
  void sameRequestIdCannotBeUsedForAnotherItem() {
    RedemptionRecord record = pendingRecord("request-4", 8004L);
    when(recordRepository.findByRequestId("request-4")).thenReturn(Optional.of(record));

    assertThatThrownBy(() -> service.redeem("POINTS_MALL", 11L, 7L, "request-4"))
        .isInstanceOf(IncentiveBusinessException.class)
        .hasMessage("兑换请求号已用于其他请求");
    verify(pointsClient, never()).debit(org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyLong(),
        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
  }

  private RedemptionRecord pendingRecord(String requestId, Long pointBusinessId) {
    IncentiveActivity activity = new IncentiveActivity(
        "POINTS_MALL", ActivityType.REDEMPTION, "积分商城", NOW.minusSeconds(3600),
        NOW.plusSeconds(3600));
    ReflectionTestUtils.setField(activity, "id", 1L);
    ParticipationRule rule = org.springframework.beans.BeanUtils.instantiateClass(ParticipationRule.class);
    ReflectionTestUtils.setField(rule, "id", 2L);
    ReflectionTestUtils.setField(rule, "ruleVersion", 1);
    RedemptionItem item = org.springframework.beans.BeanUtils.instantiateClass(RedemptionItem.class);
    ReflectionTestUtils.setField(item, "id", 10L);
    ReflectionTestUtils.setField(item, "itemCode", "WELCOME_COUPON");
    ReflectionTestUtils.setField(item, "prizeId", 101L);
    ReflectionTestUtils.setField(item, "prizeName", "新人优惠券");
    ReflectionTestUtils.setField(item, "prizeType", PrizeType.VIRTUAL);
    ReflectionTestUtils.setField(item, "pointsPrice", 50L);
    RedemptionRecord record = new RedemptionRecord(
        requestId, activity, rule, item, 7L, "{\"passed\":true}", pointBusinessId, NOW);
    ReflectionTestUtils.setField(record, "id", 71L);
    return record;
  }
}
