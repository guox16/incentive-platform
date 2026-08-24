package com.incentive.award.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.incentive.award.config.AwardMessagingConfiguration;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AwardResultPublisher {
  private final RabbitTemplate rabbitTemplate;
  private final ObjectMapper objectMapper;
  private final Duration confirmTimeout;

  public AwardResultPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper,
      @Value("${award.messaging.confirm-timeout:PT3S}") Duration confirmTimeout) {
    this.rabbitTemplate = rabbitTemplate;
    this.objectMapper = objectMapper;
    if (confirmTimeout.isZero() || confirmTimeout.isNegative()) {
      throw new IllegalArgumentException("发奖结果确认超时时间必须大于0");
    }
    this.confirmTimeout = confirmTimeout;
  }

  public void publish(AwardResultMessage result) {
    CorrelationData correlation = new CorrelationData(
        result.commandKey() + ":" + result.status());
    Message message;
    try {
      message = MessageBuilder.withBody(objectMapper.writeValueAsBytes(result))
          .setContentType("application/json")
          .setMessageId(correlation.getId())
          .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
          .build();
    } catch (JsonProcessingException ex) {
      throw new AmqpException("发奖结果序列化失败", ex);
    }
    rabbitTemplate.send(
        AwardMessagingConfiguration.RESULT_EXCHANGE,
        AwardMessagingConfiguration.RESULT_ROUTING_KEY,
        message,
        correlation);
    try {
      CorrelationData.Confirm confirm = correlation.getFuture()
          .get(confirmTimeout.toMillis(), TimeUnit.MILLISECONDS);
      if (!confirm.isAck()) {
        throw new AmqpException("RabbitMQ拒绝发奖结果: " + confirm.getReason());
      }
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new AmqpException("等待发奖结果确认时被中断", ex);
    } catch (Exception ex) {
      if (ex instanceof AmqpException amqpException) throw amqpException;
      throw new AmqpException("等待发奖结果确认失败", ex);
    }
  }
}
