package com.incentive.award.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.incentive.award.domain.AwardIssuance;
import com.incentive.award.domain.AwardIssuanceStatus;
import com.incentive.award.infrastructure.AwardDeliveryException;
import com.incentive.award.infrastructure.AwardPointsClient;
import com.incentive.award.messaging.AwardCommandMessage;
import java.io.IOException;
import org.springframework.stereotype.Service;

@Service
public class AwardIssuanceService {
  private final AwardIssuanceCoordinator coordinator;
  private final AwardIssuanceStateService stateService;
  private final AwardPointsClient pointsClient;
  private final ObjectMapper objectMapper;

  public AwardIssuanceService(AwardIssuanceCoordinator coordinator,
      AwardIssuanceStateService stateService, AwardPointsClient pointsClient,
      ObjectMapper objectMapper) {
    this.coordinator = coordinator;
    this.stateService = stateService;
    this.pointsClient = pointsClient;
    this.objectMapper = objectMapper;
  }

  public IssuanceResult issue(AwardCommandMessage command) {
    AwardIssuance issuance = coordinator.prepare(command);
    if (issuance.getStatus() == AwardIssuanceStatus.SUCCEEDED) {
      return new IssuanceResult(issuance.getId(), issuance.getResultRef(), true);
    }
    try {
      String resultRef = switch (issuance.getAwardType()) {
        case POINTS -> issuePoints(issuance);
        case VIRTUAL -> "VIRTUAL_AWARD:" + issuance.getId();
        case NONE -> throw new AwardDeliveryException(
            "AWARD_TYPE_UNSUPPORTED", "谢谢参与不能创建发奖任务");
      };
      AwardIssuance succeeded = stateService.succeed(issuance.getId(), resultRef);
      return new IssuanceResult(succeeded.getId(), succeeded.getResultRef(), false);
    } catch (RuntimeException failure) {
      String code = failure instanceof AwardDeliveryException delivery
          ? delivery.getCode() : "AWARD_ISSUE_FAILED";
      try {
        stateService.fail(issuance.getId(), code, failure.getMessage());
      } catch (RuntimeException stateFailure) {
        failure.addSuppressed(stateFailure);
      }
      throw failure;
    }
  }

  private String issuePoints(AwardIssuance issuance) {
    long points = readPoints(issuance.getAwardPayload());
    AwardPointsClient.PointCreditResult result = pointsClient.credit(
        issuance.getPointBusinessId(), issuance.getUserId(), points, issuance.getAwardName());
    return "POINT_TRANSACTION:" + result.transactionId();
  }

  private long readPoints(String payload) {
    try {
      JsonNode root = objectMapper.readTree(payload);
      JsonNode points = root == null ? null : root.get("points");
      if (points == null || !points.isIntegralNumber()
          || !points.canConvertToLong() || points.longValue() <= 0) {
        throw new AwardDeliveryException(
            "AWARD_PAYLOAD_INVALID", "积分奖品必须配置正整数points");
      }
      return points.longValue();
    } catch (IOException | IllegalArgumentException ex) {
      throw new AwardDeliveryException("AWARD_PAYLOAD_INVALID", "积分发奖参数不是合法JSON", ex);
    }
  }

  public record IssuanceResult(Long issuanceId, String resultRef, boolean replayed) {}
}
