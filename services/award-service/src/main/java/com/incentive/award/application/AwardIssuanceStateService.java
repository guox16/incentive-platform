package com.incentive.award.application;

import com.incentive.award.domain.AwardIssuance;
import com.incentive.award.repository.AwardIssuanceRepository;
import java.time.Clock;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AwardIssuanceStateService {
  private final AwardIssuanceRepository repository;
  private final Clock clock;

  public AwardIssuanceStateService(AwardIssuanceRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  public Optional<AwardIssuance> find(String commandKey) {
    return repository.findByCommandKey(commandKey);
  }

  @Transactional
  public AwardIssuance restart(Long id) {
    AwardIssuance issuance = require(id);
    issuance.restart(clock.instant());
    return issuance;
  }

  @Transactional
  public AwardIssuance succeed(Long id, String resultRef) {
    AwardIssuance issuance = require(id);
    issuance.succeed(resultRef, clock.instant());
    return issuance;
  }

  @Transactional
  public void fail(Long id, String code, String message) {
    require(id).fail(code, message, clock.instant());
  }

  private AwardIssuance require(Long id) {
    return repository.findById(id)
        .orElseThrow(() -> new IllegalStateException("发奖记录不存在: " + id));
  }
}
