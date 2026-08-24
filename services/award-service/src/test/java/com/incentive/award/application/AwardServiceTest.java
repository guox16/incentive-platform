package com.incentive.award.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.incentive.award.domain.Award;
import com.incentive.award.domain.AwardStatus;
import com.incentive.award.domain.AwardType;
import com.incentive.award.dto.AdjustInventoryRequest;
import com.incentive.award.dto.AwardUpsertRequest;
import com.incentive.award.infrastructure.BusinessNumberGenerator;
import com.incentive.award.repository.AwardInventoryLedgerRepository;
import com.incentive.award.repository.AwardRepository;
import com.incentive.award.support.AwardBusinessException;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AwardServiceTest {
  private static final Instant NOW = Instant.parse("2026-08-22T00:00:00Z");

  @Mock private AwardRepository awardRepository;
  @Mock private AwardInventoryLedgerRepository ledgerRepository;
  @Mock private BusinessNumberGenerator businessNumberGenerator;
  private AwardService service;

  @BeforeEach
  void setUp() {
    service = new AwardService(
        awardRepository, ledgerRepository, businessNumberGenerator,
        Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void createsAwardInUnifiedTable() {
    when(businessNumberGenerator.next()).thenReturn(12345L);
    when(awardRepository.save(any(Award.class))).thenAnswer(call -> {
      Award award = call.getArgument(0);
      setId(award, 1L);
      return award;
    });

    var response = service.create(request(10, 10));

    assertThat(response.id()).isEqualTo(1L);
    assertThat(response.code()).isEqualTo("PRIZE_VIRTUAL_9IX");
    assertThat(response.status()).isEqualTo(AwardStatus.ACTIVE);
    assertThat(response.availableStock()).isEqualTo(10);
  }

  @Test
  void rejectsNegativeInventoryAfterAdjustment() {
    Award award = award(1L, 3, 3);
    when(ledgerRepository.findByBusinessNo("adjust-1")).thenReturn(Optional.empty());
    when(awardRepository.findByIdAndStatusNot(1L, AwardStatus.DELETED))
        .thenReturn(Optional.of(award));

    assertThatThrownBy(() -> service.adjustInventory(
        1L, new AdjustInventoryRequest("adjust-1", -4L, "盘点")))
        .isInstanceOf(AwardBusinessException.class)
        .hasMessage("库存调整后不能小于0或超出数值范围");
  }

  @Test
  void rejectsInventoryChangeThroughAwardUpdate() {
    Award award = award(1L, 3, 3);
    when(awardRepository.findByIdAndStatusNot(1L, AwardStatus.DELETED))
        .thenReturn(Optional.of(award));

    assertThatThrownBy(() -> service.update(1L, request(4, 4)))
        .isInstanceOf(AwardBusinessException.class)
        .hasMessage("请使用库存调整接口修改库存");
  }

  @Test
  void changesStatusWithoutSendingStaleInventory() {
    Award award = award(1L, 10, 7);
    when(awardRepository.findByIdAndStatusNot(1L, AwardStatus.DELETED))
        .thenReturn(Optional.of(award));

    var response = service.updateStatus(1L, AwardStatus.INACTIVE);

    assertThat(response.status()).isEqualTo(AwardStatus.INACTIVE);
    assertThat(response.availableStock()).isEqualTo(7);
  }

  private Award award(Long id, long totalStock, long availableStock) {
    Award award = new Award("PRIZE_VIRTUAL_EXISTING", request(totalStock, availableStock), NOW);
    setId(award, id);
    return award;
  }

  private AwardUpsertRequest request(long totalStock, long availableStock) {
    return new AwardUpsertRequest(
        "5 元券", AwardType.VIRTUAL, AwardStatus.ACTIVE,
        null, "{\"coupon\":\"5\"}", totalStock, availableStock);
  }

  private void setId(Award award, Long id) {
    try {
      Field field = Award.class.getDeclaredField("id");
      field.setAccessible(true);
      field.set(award, id);
    } catch (ReflectiveOperationException ex) {
      throw new AssertionError(ex);
    }
  }
}
