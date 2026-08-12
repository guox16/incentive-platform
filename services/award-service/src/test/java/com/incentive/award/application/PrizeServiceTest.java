package com.incentive.award.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.incentive.award.domain.Prize;
import com.incentive.award.domain.PrizeStatus;
import com.incentive.award.domain.PrizeType;
import com.incentive.award.dto.AdjustInventoryRequest;
import com.incentive.award.dto.CreatePrizeRequest;
import com.incentive.award.dto.UpdatePrizeRequest;
import com.incentive.award.repository.PrizeInventoryLedgerRepository;
import com.incentive.award.repository.PrizeRepository;
import com.incentive.award.support.PrizeBusinessException;
import java.lang.reflect.Field;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PrizeServiceTest {
  @Mock PrizeRepository prizeRepository;
  @Mock PrizeInventoryLedgerRepository ledgerRepository;
  @InjectMocks PrizeService service;

  @Test
  void createsDraftPrize() {
    when(prizeRepository.existsByCode("COUPON_5")).thenReturn(false);
    when(prizeRepository.save(any(Prize.class))).thenAnswer(call -> {
      Prize prize = call.getArgument(0); setId(prize, 1L); return prize;
    });
    var response = service.create(new CreatePrizeRequest(" COUPON_5 ", "5 元券", PrizeType.VIRTUAL, 10, "{\"coupon\":\"5\"}"));
    assertThat(response.id()).isEqualTo(1L);
    assertThat(response.status()).isEqualTo(PrizeStatus.DRAFT);
    assertThat(response.availableStock()).isEqualTo(10);
  }

  @Test
  void rejectsDuplicateCode() {
    when(prizeRepository.existsByCode("COUPON_5")).thenReturn(true);
    assertThatThrownBy(() -> service.create(new CreatePrizeRequest("COUPON_5", "5 元券", PrizeType.VIRTUAL, 0, null)))
        .isInstanceOf(PrizeBusinessException.class).hasMessage("奖品编码已存在");
  }

  @Test
  void rejectsNegativeInventoryAfterAdjustment() {
    Prize prize = prize(1L, 3);
    when(prizeRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(prize));
    when(ledgerRepository.findByBusinessNo("adjust-1")).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.adjustInventory(1L, new AdjustInventoryRequest("adjust-1", -4L, "盘点")))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("库存不能小于零");
  }

  @Test
  void doesNotSoftDeleteThroughUpdate() {
    Prize prize = prize(1L, 3);
    when(prizeRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(prize));
    assertThatThrownBy(() -> service.update(1L, new UpdatePrizeRequest("券", PrizeType.VIRTUAL, PrizeStatus.DELETED, null)))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("请使用删除接口下架奖品");
  }

  private Prize prize(Long id, long stock) {
    Prize prize = new Prize("COUPON", "券", PrizeType.VIRTUAL, stock, null); setId(prize, id); return prize;
  }
  private void setId(Prize prize, Long id) {
    try { Field field = Prize.class.getDeclaredField("id"); field.setAccessible(true); field.set(prize, id); }
    catch (ReflectiveOperationException ex) { throw new AssertionError(ex); }
  }
}
