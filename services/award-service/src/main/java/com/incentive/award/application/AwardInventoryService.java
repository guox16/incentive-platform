package com.incentive.award.application;

import com.incentive.award.domain.Award;
import com.incentive.award.domain.AwardInventoryLedger;
import com.incentive.award.domain.AwardIssuance;
import com.incentive.award.domain.AwardSourceType;
import com.incentive.award.domain.ShopRedemption;
import com.incentive.award.repository.AwardInventoryLedgerRepository;
import com.incentive.award.repository.AwardRepository;
import com.incentive.award.support.AwardBusinessException;
import java.time.Clock;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 将一次成功发奖同步为真实库存扣减；发奖记录 ID 是唯一幂等键。 */
@Service
public class AwardInventoryService {
  private final AwardRepository awardRepository;
  private final AwardInventoryLedgerRepository ledgerRepository;
  private final Clock clock;

  public AwardInventoryService(AwardRepository awardRepository,
      AwardInventoryLedgerRepository ledgerRepository, Clock clock) {
    this.awardRepository = awardRepository;
    this.ledgerRepository = ledgerRepository;
    this.clock = clock;
  }

  @Transactional
  public void consume(AwardIssuance issuance) {
    if (issuance.getSourceType() == AwardSourceType.SHOP_REDEMPTION) {
      AwardInventoryLedger reservation = ledgerRepository
          .findByBusinessNo(reservationKey(issuance.getSourceRecordId()))
          .orElseThrow(() -> new IllegalStateException("兑换单缺少库存预占流水"));
      if (!reservation.getAwardId().equals(issuance.getAwardId())
          || ledgerRepository.findByBusinessNo(releaseKey(issuance.getSourceRecordId())).isPresent()) {
        throw new IllegalStateException("兑换单的库存预占无效");
      }
      return;
    }
    String businessNo = "ISSUANCE:" + issuance.getId();
    AwardInventoryLedger existing = ledgerRepository.findByBusinessNo(businessNo).orElse(null);
    if (existing != null) {
      if (!existing.getAwardId().equals(issuance.getAwardId())
          || existing.getChangeAmount() != -1L) {
        throw new AwardBusinessException("INVENTORY_BUSINESS_NO_CONFLICT",
            "发奖库存流水与发奖记录不一致", HttpStatus.CONFLICT);
      }
      return;
    }

    Award award = awardRepository.findByIdWithPessimisticLock(issuance.getAwardId())
        .orElseThrow(() -> new AwardBusinessException("AWARD_NOT_FOUND",
            "发奖奖品不存在", HttpStatus.NOT_FOUND));
    try {
      award.consumeInventory(clock.instant());
    } catch (IllegalArgumentException ex) {
      throw new AwardBusinessException("AWARD_INVENTORY_INSUFFICIENT",
          "奖品库存不足，无法完成发奖", HttpStatus.CONFLICT);
    }
    ledgerRepository.save(new AwardInventoryLedger(award.getId(), businessNo, -1L,
        award.getAvailableStock(), "发奖扣减：" + issuance.getCommandKey(), clock.instant()));
  }

  void reserveForRedemption(Award award, Long redemptionId, Instant now) {
    award.consumeInventory(now);
    ledgerRepository.save(new AwardInventoryLedger(award.getId(), reservationKey(redemptionId),
        "RESERVE", -1L, award.getAvailableStock(), "商城兑换预占", now));
  }

  void releaseRedemption(ShopRedemption redemption, Instant now) {
    Award award = awardRepository.findByIdWithPessimisticLock(redemption.getAwardId())
        .orElseThrow(() -> new IllegalStateException("兑换奖品不存在"));
    long before = award.getAvailableStock();
    award.releaseInventory(now);
    ledgerRepository.save(new AwardInventoryLedger(award.getId(), releaseKey(redemption.getId()),
        "RELEASE", award.getAvailableStock() - before, award.getAvailableStock(),
        "商城兑换积分不足，释放库存", now));
  }

  private static String reservationKey(Long id) { return "SHOP_RESERVE:" + id; }

  private static String releaseKey(Long id) { return "SHOP_RELEASE:" + id; }
}
