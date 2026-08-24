package com.incentive.award.application;

import com.incentive.award.domain.AwardIssuance;
import com.incentive.award.domain.AwardIssuanceStatus;
import com.incentive.award.domain.AwardType;
import com.incentive.award.domain.UserAward;
import com.incentive.award.repository.AwardIssuanceRepository;
import com.incentive.award.repository.UserAwardRepository;
import java.time.Clock;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AwardIssuanceStateService {
  private final AwardIssuanceRepository repository;
  private final UserAwardRepository userAwardRepository;
  private final AwardInventoryService inventoryService;
  private final Clock clock;

  public AwardIssuanceStateService(AwardIssuanceRepository repository,
      UserAwardRepository userAwardRepository, AwardInventoryService inventoryService, Clock clock) {
    this.repository = repository;
    this.userAwardRepository = userAwardRepository;
    this.inventoryService = inventoryService;
    this.clock = clock;
  }

  public Optional<AwardIssuance> find(String commandKey) {
    return repository.findByCommandKey(commandKey);
  }

  @Transactional
  public AwardIssuance restart(Long id) {
    AwardIssuance issuance = requireForUpdate(id);
    issuance.restart(clock.instant());
    return issuance;
  }

  @Transactional
  public AwardIssuance succeed(Long id, String deliveryResultRef) {
    AwardIssuance issuance = requireForUpdate(id);
    if (issuance.getStatus() == AwardIssuanceStatus.SUCCEEDED) return issuance;
    inventoryService.consume(issuance);
    UserAward userAward = userAwardRepository.findByIssuanceId(id)
        .orElseGet(() -> userAwardRepository.saveAndFlush(
            new UserAward(issuance, clock.instant())));
    String resultRef = issuance.getAwardType() == AwardType.VIRTUAL
        ? "USER_AWARD:" + userAward.getId() : deliveryResultRef;
    issuance.succeed(resultRef, clock.instant());
    return issuance;
  }

  @Transactional
  public void fail(Long id, String code, String message) {
    requireForUpdate(id).fail(code, message, clock.instant());
  }

  private AwardIssuance requireForUpdate(Long id) {
    return repository.findByIdForUpdate(id)
        .orElseThrow(() -> new IllegalStateException("发奖记录不存在: " + id));
  }
}
