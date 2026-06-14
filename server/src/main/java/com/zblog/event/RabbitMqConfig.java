package com.zblog.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@EnableRabbit
@Profile("!test")
public class RabbitMqConfig {

  @Bean
  DirectExchange zblogEventsExchange(EventProperties properties) {
    return new DirectExchange(properties.getExchange(), true, false);
  }

  @Bean
  Queue articlePublishedQueue(EventProperties properties) {
    return new Queue(properties.getArticlePublishedQueue(), true);
  }

  @Bean
  Queue searchIndexQueue(EventProperties properties) {
    return new Queue(properties.getSearchIndexQueue(), true);
  }

  @Bean
  Queue commentReplyQueue(EventProperties properties) {
    return new Queue(properties.getCommentReplyQueue(), true);
  }

  @Bean
  Binding articlePublishedBinding(DirectExchange zblogEventsExchange, Queue articlePublishedQueue, EventProperties properties) {
    return BindingBuilder.bind(articlePublishedQueue)
        .to(zblogEventsExchange)
        .with(properties.getArticlePublishedRoutingKey());
  }

  @Bean
  Binding searchIndexBinding(DirectExchange zblogEventsExchange, Queue searchIndexQueue, EventProperties properties) {
    return BindingBuilder.bind(searchIndexQueue)
        .to(zblogEventsExchange)
        .with(properties.getSearchIndexRoutingKey());
  }

  @Bean
  Binding commentReplyBinding(DirectExchange zblogEventsExchange, Queue commentReplyQueue, EventProperties properties) {
    return BindingBuilder.bind(commentReplyQueue)
        .to(zblogEventsExchange)
        .with(properties.getCommentReplyRoutingKey());
  }

  @Bean
  Jackson2JsonMessageConverter rabbitJsonMessageConverter(ObjectMapper objectMapper) {
    return new Jackson2JsonMessageConverter(objectMapper);
  }

  @Bean
  RabbitTemplate rabbitTemplate(
      ConnectionFactory connectionFactory, Jackson2JsonMessageConverter rabbitJsonMessageConverter) {
    RabbitTemplate template = new RabbitTemplate(connectionFactory);
    template.setMessageConverter(rabbitJsonMessageConverter);
    return template;
  }

  @Bean
  SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
      ConnectionFactory connectionFactory, Jackson2JsonMessageConverter rabbitJsonMessageConverter) {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);
    factory.setMessageConverter(rabbitJsonMessageConverter);
    return factory;
  }
}
