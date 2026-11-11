package com.incentive.award.application;

import com.incentive.award.domain.Award;
import com.incentive.award.domain.AwardStatus;
import com.incentive.award.domain.AwardType;
import com.incentive.award.domain.ShopRedemption;
import com.incentive.award.domain.ShopRedemption.Status;
import com.incentive.award.infrastructure.AwardDeliveryException;
import com.incentive.award.infrastructure.AwardPointsClient;
import com.incentive.award.repository.AwardRepository;
import com.incentive.award.repository.ShopRedemptionRepository;
import com.incentive.award.support.AwardBusinessException;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShopRedemptionTransactions {
  private final ShopRedemptionRepository redemptions;
  private final AwardRepository awards;
  private final AwardInventoryService inventory;
  private final AwardPointsClient points;
  private final AwardIssuanceService issuance;
  private final EntityManager entityManager;
  private final Clock clock;

  public ShopRedemptionTransactions(ShopRedemptionRepository redemptions, AwardRepository awards,
      AwardInventoryService inventory, AwardPointsClient points, AwardIssuanceService issuance,
      EntityManager entityManager, Clock clock) {
    this.redemptions = redemptions;
    this.awards = awards;
    this.inventory = inventory;
    this.points = points;
    this.issuance = issuance;
    this.entityManager = entityManager;
    this.clock = clock;
  }

  @Transactional
  public ShopRedemption reserve(Long id, Long userId, String requestId, Long awardId, long expectedPoints) {
    Award award = awards.findByIdWithPessimisticLock(awardId)
        .orElseThrow(() -> unavailable());
    // Recheck after the stock lock so a concurrent retry can replay the last item.
    ShopRedemption existing = redemptions.findByUserIdAndRequestId(userId, requestId).orElse(null);
    if (existing != null) return existing;
    if (!award.isRedemptionEnabled() || award.getStatus() != AwardStatus.ACTIVE
        || award.getType() == AwardType.NONE || award.getRedemptionPointsPrice() == null
        || award.getRedemptionPointsPrice() <= 0 || award.getAvailableStock() <= 0) {
      throw unavailable();
    }
    if (award.getRedemptionPointsPrice() != expectedPoints) {
      throw new AwardBusinessException("REDEMPTION_PRICE_CHANGED",
          "兑换价格已变更，请刷新商城后重试", HttpStatus.CONFLICT);
    }
    if (award.getType() == AwardType.POINTS) {
      try {
        issuance.readPoints(award.getAwardPayload());
      } catch (AwardDeliveryException invalid) {
        throw new AwardBusinessException(invalid.getCode(), invalid.getMessage(), HttpStatus.CONFLICT);
      }
    }

    Instant now = clock.instant();
    ShopRedemption redemption = new ShopRedemption(id, userId, requestId, award, now);
    entityManager.persist(redemption);
    entityManager.flush();
    inventory.reserveForRedemption(award, id, now);
    return redemption;
  }

  @Transactional
  public ShopRedemption charge(Long id) {
    ShopRedemption redemption = requireForUpdate(id);
    if (redemption.getStatus() != Status.RESERVED) return redemption;

    // A single caller decides whether to release stock or commit the debit result.
    Optional<AwardPointsClient.PointDebitResult> debit = points.debit(id, redemption.getUserId(),
        redemption.getPointsCost(), redemption.getAwardName());
    if (debit.isPresent()) {
      redemption.pay(debit.get().transactionId(), debit.get().balanceAfter());
    } else {
      inventory.releaseRedemption(redemption, clock.instant());
      redemption.reject();
    }
    return redemption;
  }

  @Transactional
  public ShopRedemption complete(Long id) {
    ShopRedemption redemption = requireForUpdate(id);
    if (redemption.getStatus() == Status.PAID) redemption.complete();
    return redemption;
  }

  @Transactional
  public ShopRedemption defer(Long id) {
    ShopRedemption redemption = requireForUpdate(id);
    if (redemption.getStatus() == Status.RESERVED || redemption.getStatus() == Status.PAID) {
      redemption.defer(clock.instant().plusSeconds(30));
    }
    return redemption;
  }

  private ShopRedemption requireForUpdate(Long id) {
    return redemptions.findForUpdate(id).orElseThrow();
  }

  private static AwardBusinessException unavailable() {
    return new AwardBusinessException("REDEMPTION_UNAVAILABLE",
        "商品已售罄或下架，请刷新商城", HttpStatus.CONFLICT);
  }
}
