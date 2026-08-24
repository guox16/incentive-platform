package com.incentive.activity.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incentive.activity.application.PendingAwardResultService;
import com.incentive.activity.config.AwardMessagingConfiguration;
import com.incentive.activity.infrastructure.AwardResultMessage;
import java.io.IOException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class AwardResultConsumer {
  private final ObjectMapper objectMapper;
  private final PendingAwardResultService resultService;

  public AwardResultConsumer(ObjectMapper objectMapper, PendingAwardResultService resultService) {
    this.objectMapper = objectMapper;
    this.resultService = resultService;
  }

  @RabbitListener(queues = AwardMessagingConfiguration.RESULT_QUEUE)
  public void consume(Message message) {
    try {
      resultService.apply(
          objectMapper.readValue(message.getBody(), AwardResultMessage.class));
    } catch (IOException ex) {
      throw new IllegalArgumentException("发奖结果消息格式错误", ex);
    }
  }
}
