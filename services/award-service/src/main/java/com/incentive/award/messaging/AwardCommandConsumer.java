package com.incentive.award.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incentive.award.application.AwardIssuanceService;
import com.incentive.award.config.AwardMessagingConfiguration;
import com.incentive.award.infrastructure.AwardDeliveryException;
import java.io.IOException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class AwardCommandConsumer {
  private final ObjectMapper objectMapper;
  private final AwardIssuanceService issuanceService;
  private final AwardResultPublisher resultPublisher;

  public AwardCommandConsumer(ObjectMapper objectMapper,
      AwardIssuanceService issuanceService, AwardResultPublisher resultPublisher) {
    this.objectMapper = objectMapper;
    this.issuanceService = issuanceService;
    this.resultPublisher = resultPublisher;
  }

  @RabbitListener(queues = AwardMessagingConfiguration.COMMAND_QUEUE)
  public void consume(Message message) {
    AwardCommandMessage command = read(message);
    AwardIssuanceService.IssuanceResult result;
    try {
      result = issuanceService.issue(command);
    } catch (RuntimeException failure) {
      String code = failure instanceof AwardDeliveryException delivery
          ? delivery.getCode() : "AWARD_ISSUE_FAILED";
      resultPublisher.publish(
          AwardResultMessage.failed(command, code, failure.getMessage()));
      return;
    }
    resultPublisher.publish(AwardResultMessage.awarded(command, result.resultRef()));
  }

  private AwardCommandMessage read(Message message) {
    try {
      return objectMapper.readValue(message.getBody(), AwardCommandMessage.class);
    } catch (IOException ex) {
      throw new IllegalArgumentException("发奖命令消息格式错误", ex);
    }
  }
}
