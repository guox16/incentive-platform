package com.incentive.activity.infrastructure;

import com.incentive.activity.config.AwardMessagingConfiguration;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AwardMessagePublisher {
  private final RabbitTemplate rabbitTemplate;
  private final Duration confirmTimeout;

  public AwardMessagePublisher(RabbitTemplate rabbitTemplate,
      @Value("${award.dispatch.confirm-timeout:PT3S}") Duration confirmTimeout) {
    if (confirmTimeout.isNegative() || confirmTimeout.isZero()) {
      throw new IllegalArgumentException("发奖消息确认超时时间必须大于0");
    }
    this.rabbitTemplate = rabbitTemplate;
    this.confirmTimeout = confirmTimeout;
  }

  public void publish(AwardDispatchMessage message) {
    CorrelationData correlation = new CorrelationData(message.commandKey());
    rabbitTemplate.convertAndSend(
        AwardMessagingConfiguration.COMMAND_EXCHANGE,
        AwardMessagingConfiguration.COMMAND_ROUTING_KEY,
        message,
        amqpMessage -> {
          amqpMessage.getMessageProperties().setMessageId(message.commandKey());
          amqpMessage.getMessageProperties().setContentType("application/json");
          return amqpMessage;
        },
        correlation);
    try {
      CorrelationData.Confirm confirm = correlation.getFuture()
          .get(confirmTimeout.toMillis(), TimeUnit.MILLISECONDS);
      if (!confirm.isAck()) {
        throw new AmqpException("RabbitMQ拒绝发奖消息: " + confirm.getReason());
      }
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new AmqpException("等待RabbitMQ确认时被中断", ex);
    } catch (Exception ex) {
      if (ex instanceof AmqpException amqpException) throw amqpException;
      throw new AmqpException("等待RabbitMQ确认失败", ex);
    }
  }
}
