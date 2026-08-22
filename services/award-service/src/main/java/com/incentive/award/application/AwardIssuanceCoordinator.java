package com.incentive.award.application;

import com.incentive.award.domain.AwardIssuance;
import com.incentive.award.domain.AwardIssuanceStatus;
import com.incentive.award.messaging.AwardCommandMessage;
import java.util.Objects;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class AwardIssuanceCoordinator {
  private final AwardIssuanceCreationService creationService;
  private final AwardIssuanceStateService stateService;

  public AwardIssuanceCoordinator(AwardIssuanceCreationService creationService,
      AwardIssuanceStateService stateService) {
    this.creationService = creationService;
    this.stateService = stateService;
  }

  public AwardIssuance prepare(AwardCommandMessage command) {
    String expectedCommandKey = command.sourceType() + ":" + command.sourceRecordId();
    if (!expectedCommandKey.equals(command.commandKey())) {
      throw new AwardCommandConflictException("发奖幂等号与来源记录不一致");
    }
    AwardIssuance issuance = stateService.find(command.commandKey()).orElse(null);
    if (issuance == null) {
      try {
        issuance = creationService.create(command);
      } catch (DataIntegrityViolationException duplicate) {
        issuance = stateService.find(command.commandKey()).orElseThrow(() -> duplicate);
      }
    }
    validateSameCommand(issuance, command);
    return issuance.getStatus() == AwardIssuanceStatus.FAILED
        ? stateService.restart(issuance.getId()) : issuance;
  }

  private void validateSameCommand(AwardIssuance issuance, AwardCommandMessage command) {
    boolean same = issuance.getSourceType() == command.sourceType()
        && Objects.equals(issuance.getSourceRecordId(), command.sourceRecordId())
        && Objects.equals(issuance.getUserId(), command.userId())
        && Objects.equals(issuance.getAwardId(), command.awardId())
        && Objects.equals(issuance.getAwardName(), command.awardName())
        && issuance.getAwardType() == command.awardType()
        && Objects.equals(issuance.getAwardPayload(), command.awardPayload())
        && Objects.equals(issuance.getStockNo(), command.stockNo());
    if (!same) {
      throw new AwardCommandConflictException("相同幂等号对应的发奖参数不一致");
    }
  }
}
