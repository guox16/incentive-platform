package com.incentive.award.application;

import com.incentive.award.domain.ShopRedemption;
import com.incentive.award.domain.ShopRedemption.Status;
import com.incentive.award.dto.ShopRedemptionResponse;
import com.incentive.award.infrastructure.BusinessNumberGenerator;
import com.incentive.award.repository.ShopRedemptionRepository;
import com.incentive.award.support.AwardBusinessException;
import java.time.Clock;
import java.util.EnumSet;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ShopRedemptionService {
  private static final Logger log = LoggerFactory.getLogger(ShopRedemptionService.class);
  private static final PageRequest BATCH = PageRequest.of(0, 20);

  private final ShopRedemptionRepository repository;
  private final ShopRedemptionTransactions transactions;
  private final AwardIssuanceService issuance;
  private final BusinessNumberGenerator numbers;
  private final Clock clock;

  public ShopRedemptionService(ShopRedemptionRepository repository,
      ShopRedemptionTransactions transactions, AwardIssuanceService issuance,
      BusinessNumberGenerator numbers, Clock clock) {
    this.repository = repository;
    this.transactions = transactions;
    this.issuance = issuance;
    this.numbers = numbers;
    this.clock = clock;
  }

  public ShopRedemptionResponse redeem(Long userId, String requestId, Long awardId, long expectedPoints) {
    String key = requestId.trim();
    ShopRedemption redemption = repository.findByUserIdAndRequestId(userId, key)
        .orElseGet(() -> reserve(userId, key, awardId, expectedPoints));
    if (!redemption.getAwardId().equals(awardId) || redemption.getPointsCost() != expectedPoints) {
      throw new AwardBusinessException("REDEMPTION_REQUEST_ID_REUSED",
          "兑换请求号已用于其他请求", HttpStatus.CONFLICT);
    }
    if (redemption.getStatus() == Status.RESERVED || redemption.getStatus() == Status.PAID) {
      redemption = advance(redemption.getId());
    }
    return ShopRedemptionResponse.from(redemption);
  }

  public List<ShopRedemptionResponse> recent(Long userId) {
    return repository.findByUserIdOrderByIdDesc(userId, BATCH).stream()
        .map(ShopRedemptionResponse::from).toList();
  }

  @Scheduled(fixedDelay = 5000)
  public void resumePending() {
    for (Long id : repository.findDueIds(EnumSet.of(Status.RESERVED, Status.PAID), clock.instant(), BATCH)) {
      try {
        advance(id);
      } catch (RuntimeException failure) {
        log.warn("无法更新待处理兑换单 {}", id, failure);
      }
    }
  }

  private ShopRedemption reserve(Long userId, String key, Long awardId, long expectedPoints) {
    try {
      return transactions.reserve(numbers.next(), userId, key, awardId, expectedPoints);
    } catch (DataIntegrityViolationException race) {
      return repository.findByUserIdAndRequestId(userId, key).orElseThrow(() -> race);
    }
  }

  private ShopRedemption advance(Long id) {
    try {
      ShopRedemption redemption = transactions.charge(id);
      if (redemption.getStatus() != Status.PAID) return redemption;
      issuance.issue(redemption.issuanceCommand());
      return transactions.complete(id);
    } catch (RuntimeException failure) {
      log.warn("兑换单 {} 稍后继续处理", id, failure);
      return transactions.defer(id);
    }
  }
}
