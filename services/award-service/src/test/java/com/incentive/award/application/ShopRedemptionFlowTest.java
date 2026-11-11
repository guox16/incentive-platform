package com.incentive.award.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.incentive.award.domain.Award;
import com.incentive.award.domain.AwardStatus;
import com.incentive.award.domain.AwardType;
import com.incentive.award.dto.AwardUpsertRequest;
import com.incentive.award.dto.ShopRedemptionResponse.Status;
import com.incentive.award.infrastructure.AwardDeliveryException;
import com.incentive.award.infrastructure.AwardPointsClient;
import com.incentive.award.infrastructure.BusinessNumberGenerator;
import com.incentive.award.repository.AwardRepository;
import com.incentive.award.repository.UserAwardRepository;
import com.incentive.award.support.AwardBusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest(showSql = false, properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@ContextConfiguration(classes = ShopRedemptionFlowTest.TestConfig.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ShopRedemptionFlowTest {
  private static final Instant NOW = Instant.parse("2026-11-11T00:00:00Z");

  @Autowired private ShopRedemptionService service;
  @Autowired private AwardRepository awards;
  @Autowired private UserAwardRepository userAwards;
  @MockBean private AwardPointsClient points;

  @Test
  void retriesAnAmbiguousDebitWithTheSameBusinessNumberAndIssuesOnlyOnce() {
    Award award = createAward("VOUCHER");
    long initialCount = userAwards.count();
    when(points.debit(anyLong(), eq(21L), eq(10L), eq("兑换券")))
        .thenThrow(new AwardDeliveryException("TIMEOUT", "超时"))
        .thenReturn(Optional.of(new AwardPointsClient.PointDebitResult(12L, 90L)));

    var pending = service.redeem(21L, "order-1", award.getId(), 10L);
    assertThat(pending.status()).isEqualTo(Status.PROCESSING);
    assertThat(awards.findById(award.getId()).orElseThrow().getAvailableStock()).isZero();
    assertThatThrownBy(() -> service.redeem(22L, "order-2", award.getId(), 10L))
        .isInstanceOf(AwardBusinessException.class);

    var completed = service.redeem(21L, "order-1", award.getId(), 10L);
    assertThat(completed.status()).isEqualTo(Status.COMPLETED);
    assertThat(completed.id()).isEqualTo(pending.id());
    assertThat(service.redeem(21L, "order-1", award.getId(), 10L)).isEqualTo(completed);
    assertThat(service.recent(21L)).containsExactly(completed);
    assertThat(service.recent(22L)).isEmpty();
    verify(points, times(2)).debit(Long.valueOf(completed.id()), 21L, 10L, "兑换券");
    assertThat(userAwards.count()).isEqualTo(initialCount + 1);
    assertThat(awards.findById(award.getId()).orElseThrow().getAvailableStock()).isZero();
  }

  @Test
  void releasesInventoryOnlyWhenPointsServiceConfirmsInsufficientBalance() {
    Award award = createAward("REFUSED");
    when(points.debit(anyLong(), eq(23L), eq(10L), eq("兑换券"))).thenReturn(Optional.empty());

    var rejected = service.redeem(23L, "order-3", award.getId(), 10L);
    assertThat(rejected.status()).isEqualTo(Status.REJECTED);
    assertThat(service.redeem(23L, "order-3", award.getId(), 10L)).isEqualTo(rejected);
    verify(points).debit(Long.valueOf(rejected.id()), 23L, 10L, "兑换券");
    assertThat(awards.findById(award.getId()).orElseThrow().getAvailableStock()).isEqualTo(1);
  }

  private Award createAward(String code) {
    return awards.saveAndFlush(new Award(code, new AwardUpsertRequest("兑换券",
        AwardType.VIRTUAL, AwardStatus.ACTIVE, null, null, 1, 1, true, 10L), NOW));
  }

  @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
  @EntityScan(basePackageClasses = Award.class)
  @EnableJpaRepositories(basePackageClasses = AwardRepository.class)
  @Import({ShopRedemptionService.class, ShopRedemptionTransactions.class,
      AwardInventoryService.class, AwardIssuanceService.class, AwardIssuanceStateService.class,
      AwardIssuanceCoordinator.class, AwardIssuanceCreationService.class, BusinessNumberGenerator.class})
  static class TestConfig {
    @Bean Clock clock() { return Clock.fixed(NOW, ZoneOffset.UTC); }
    @Bean ObjectMapper objectMapper() { return new ObjectMapper(); }
  }
}
