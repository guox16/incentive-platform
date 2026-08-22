package com.incentive.activity.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;

@Configuration(proxyBeanMethods = false)
public class AwardMessagingConfiguration {
  public static final String COMMAND_EXCHANGE = "award.command.exchange";
  public static final String COMMAND_QUEUE = "award.issue.queue";
  public static final String COMMAND_ROUTING_KEY = "award.issue";
  public static final String DEAD_LETTER_EXCHANGE = "award.command.dlx";
  public static final String DEAD_LETTER_QUEUE = "award.issue.dlq";
  public static final String DEAD_LETTER_ROUTING_KEY = "award.issue.dead";

  @Bean
  DirectExchange awardCommandExchange() {
    return new DirectExchange(COMMAND_EXCHANGE, true, false);
  }

  @Bean
  Queue awardCommandQueue() {
    return QueueBuilder.durable(COMMAND_QUEUE)
        .deadLetterExchange(DEAD_LETTER_EXCHANGE)
        .deadLetterRoutingKey(DEAD_LETTER_ROUTING_KEY)
        .build();
  }

  @Bean
  Binding awardCommandBinding(
      @Qualifier("awardCommandQueue") Queue awardCommandQueue,
      @Qualifier("awardCommandExchange") DirectExchange awardCommandExchange) {
    return BindingBuilder.bind(awardCommandQueue)
        .to(awardCommandExchange).with(COMMAND_ROUTING_KEY);
  }

  @Bean
  DirectExchange awardDeadLetterExchange() {
    return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
  }

  @Bean
  Queue awardDeadLetterQueue() {
    return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
  }

  @Bean
  Binding awardDeadLetterBinding(
      @Qualifier("awardDeadLetterQueue") Queue awardDeadLetterQueue,
      @Qualifier("awardDeadLetterExchange") DirectExchange awardDeadLetterExchange) {
    return BindingBuilder.bind(awardDeadLetterQueue)
        .to(awardDeadLetterExchange).with(DEAD_LETTER_ROUTING_KEY);
  }

  @Bean
  Jackson2JsonMessageConverter awardMessageConverter() {
    return new Jackson2JsonMessageConverter();
  }
}
