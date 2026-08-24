package com.incentive.award.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AwardMessagingConfiguration {
  public static final String COMMAND_EXCHANGE = "award.command.exchange";
  public static final String COMMAND_QUEUE = "award.issue.queue";
  public static final String COMMAND_ROUTING_KEY = "award.issue";
  public static final String DEAD_LETTER_EXCHANGE = "award.command.dlx";
  public static final String DEAD_LETTER_QUEUE = "award.issue.dlq";
  public static final String DEAD_LETTER_ROUTING_KEY = "award.issue.dead";
  public static final String RESULT_EXCHANGE = "award.result.exchange";
  public static final String RESULT_QUEUE = "award.result.incentive.queue";
  public static final String RESULT_ROUTING_KEY = "award.result";
  public static final String RESULT_DEAD_LETTER_QUEUE = "award.result.incentive.dlq";
  public static final String RESULT_DEAD_LETTER_ROUTING_KEY = "award.result.dead";

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
      @Qualifier("awardCommandQueue") Queue queue,
      @Qualifier("awardCommandExchange") DirectExchange exchange) {
    return BindingBuilder.bind(queue).to(exchange).with(COMMAND_ROUTING_KEY);
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
      @Qualifier("awardDeadLetterQueue") Queue queue,
      @Qualifier("awardDeadLetterExchange") DirectExchange exchange) {
    return BindingBuilder.bind(queue).to(exchange).with(DEAD_LETTER_ROUTING_KEY);
  }

  @Bean
  DirectExchange awardResultExchange() {
    return new DirectExchange(RESULT_EXCHANGE, true, false);
  }

  @Bean
  Queue awardResultQueue() {
    return QueueBuilder.durable(RESULT_QUEUE)
        .deadLetterExchange(DEAD_LETTER_EXCHANGE)
        .deadLetterRoutingKey(RESULT_DEAD_LETTER_ROUTING_KEY)
        .build();
  }

  @Bean
  Binding awardResultBinding(
      @Qualifier("awardResultQueue") Queue queue,
      @Qualifier("awardResultExchange") DirectExchange exchange) {
    return BindingBuilder.bind(queue).to(exchange).with(RESULT_ROUTING_KEY);
  }

  @Bean
  Queue awardResultDeadLetterQueue() {
    return QueueBuilder.durable(RESULT_DEAD_LETTER_QUEUE).build();
  }

  @Bean
  Binding awardResultDeadLetterBinding(
      @Qualifier("awardResultDeadLetterQueue") Queue queue,
      @Qualifier("awardDeadLetterExchange") DirectExchange exchange) {
    return BindingBuilder.bind(queue).to(exchange).with(RESULT_DEAD_LETTER_ROUTING_KEY);
  }
}
