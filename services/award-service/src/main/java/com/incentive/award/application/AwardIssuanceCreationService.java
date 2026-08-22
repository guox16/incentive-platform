package com.incentive.award.application;

import com.incentive.award.domain.AwardIssuance;
import com.incentive.award.domain.AwardType;
import com.incentive.award.infrastructure.BusinessNumberGenerator;
import com.incentive.award.messaging.AwardCommandMessage;
import com.incentive.award.repository.AwardIssuanceRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AwardIssuanceCreationService {
  private final AwardIssuanceRepository repository;
  private final BusinessNumberGenerator businessNumberGenerator;
  private final Clock clock;

  public AwardIssuanceCreationService(AwardIssuanceRepository repository,
      BusinessNumberGenerator businessNumberGenerator, Clock clock) {
    this.repository = repository;
    this.businessNumberGenerator = businessNumberGenerator;
    this.clock = clock;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public AwardIssuance create(AwardCommandMessage command) {
    Long pointBusinessId = command.awardType() == AwardType.POINTS
        ? businessNumberGenerator.next() : null;
    return repository.saveAndFlush(
        new AwardIssuance(command, pointBusinessId, clock.instant()));
  }
}
