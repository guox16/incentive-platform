package com.incentive.award.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.incentive.award.domain.Award;
import com.incentive.award.domain.AwardInventoryLedger;
import com.incentive.award.domain.AwardIssuance;
import com.incentive.award.dto.AwardUpsertRequest;
import com.incentive.award.repository.AwardInventoryLedgerRepository;
import com.incentive.award.repository.AwardRepository;
import com.incentive.award.support.AwardBusinessException;
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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AwardInventoryServiceTest {
  private static final Instant NOW = Instant.parse("2026-08-24T08:00:00Z");

  @Mock private AwardRepository awardRepository;
  @Mock private AwardInventoryLedgerRepository ledgerRepository;
  private AwardInventoryService service;

  @BeforeEach
  void setUp() {
    service = new AwardInventoryService(awardRepository, ledgerRepository,
        Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void consumesOneAvailableStockAndWritesIdempotentLedgerForSuccessfulIssuance() {
    Award award = award(101L, 10L, 4L);
    AwardIssuance issuance = issuance(81L, 101L);
    when(ledgerRepository.findByBusinessNo("ISSUANCE:81")).thenReturn(Optional.empty());
    when(awardRepository.findByIdWithPessimisticLock(101L)).thenReturn(Optional.of(award));

    service.consume(issuance);

    assertThat(award.getTotalStock()).isEqualTo(10L);
    assertThat(award.getAvailableStock()).isEqualTo(3L);
    ArgumentCaptor<AwardInventoryLedger> ledger = ArgumentCaptor.forClass(AwardInventoryLedger.class);
    verify(ledgerRepository).save(ledger.capture());
    assertThat(ledger.getValue().getBusinessNo()).isEqualTo("ISSUANCE:81");
    assertThat(ledger.getValue().getOperationType()).isEqualTo("DECREASE");
    assertThat(ledger.getValue().getChangeAmount()).isEqualTo(-1L);
    assertThat(ledger.getValue().getAvailableAfter()).isEqualTo(3L);
  }

  @Test
  void replaysDoNotConsumeInventoryTwice() {
    AwardIssuance issuance = issuance(81L, 101L);
    AwardInventoryLedger existing = org.mockito.Mockito.mock(AwardInventoryLedger.class);
    when(existing.getAwardId()).thenReturn(101L);
    when(existing.getChangeAmount()).thenReturn(-1L);
    when(ledgerRepository.findByBusinessNo("ISSUANCE:81")).thenReturn(Optional.of(existing));

    service.consume(issuance);

    verify(awardRepository, org.mockito.Mockito.never()).findByIdWithPessimisticLock(any());
  }

  @Test
  void rejectsIssuanceWhenAvailableStockIsExhausted() {
    Award award = award(101L, 10L, 0L);
    AwardIssuance issuance = issuance(81L, 101L);
    when(ledgerRepository.findByBusinessNo("ISSUANCE:81")).thenReturn(Optional.empty());
    when(awardRepository.findByIdWithPessimisticLock(101L)).thenReturn(Optional.of(award));

    assertThatThrownBy(() -> service.consume(issuance))
        .isInstanceOf(AwardBusinessException.class)
        .hasMessage("奖品库存不足，无法完成发奖");

    verify(ledgerRepository, org.mockito.Mockito.never()).save(any());
  }

  private Award award(Long id, long totalStock, long availableStock) {
    Award award = new Award("PRIZE_VIRTUAL_EXISTING", new AwardUpsertRequest("优惠券",
        com.incentive.award.domain.AwardType.VIRTUAL,
        com.incentive.award.domain.AwardStatus.ACTIVE, null, "{}", totalStock, availableStock,
        false, null), NOW);
    ReflectionTestUtils.setField(award, "id", id);
    return award;
  }

  private AwardIssuance issuance(Long id, Long awardId) {
    AwardIssuance issuance = org.mockito.Mockito.mock(AwardIssuance.class);
    when(issuance.getId()).thenReturn(id);
    when(issuance.getAwardId()).thenReturn(awardId);
    return issuance;
  }
}
