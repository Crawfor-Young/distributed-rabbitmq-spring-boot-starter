package com.young;

import com.young.core.MessageInfo;
import com.young.core.RabbitRecoveryCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.retry.policy.CircuitBreakerRetryPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

@Configuration
@ConditionalOnBean(RabbitService.class)
@EnableConfigurationProperties(RabbitProperties.class)
public class RabbitAutoConfiguration {

  public static final Logger logger = LoggerFactory.getLogger("com.young.rabbit");

  private RabbitProperties rabbitProperties;

  @Bean
  @ConditionalOnBean({RabbitService.class})
  public ConnectionFactory connectionFactory() {
    CachingConnectionFactory connectionFactory = new CachingConnectionFactory(
        rabbitProperties.getHost(), rabbitProperties.getPort());
    connectionFactory.setUsername(rabbitProperties.getUsername());
    connectionFactory.setPassword(rabbitProperties.getPassword());
    connectionFactory.setVirtualHost(rabbitProperties.getVirtualHost());
    connectionFactory.setPublisherConfirms(true);
    return connectionFactory;
  }

  @Bean
  @ConditionalOnMissingBean(MessageConverter.class)
  @ConditionalOnBean(RabbitService.class)
  public MessageConverter jsonMessageConverter() {
    return new Jackson2JsonMessageConverter();
  }

  @Bean
  @ConditionalOnMissingBean(RabbitTemplate.class)
  @ConditionalOnBean(RabbitService.class)
  public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
      RetryTemplate rabbitRetryTemplate) {
    RabbitTemplate template = new RabbitTemplate(connectionFactory);
    template.setMessageConverter(jsonMessageConverter());
    template.setMandatory(true);
    template.setConfirmCallback((correlationData, ack, cause) -> {
      if (ack) {
        RabbitService.ackMap.remove(correlationData.getId());
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug(correlationData.getId() + " message ack failed");
        }
      }
    });
    template.setReturnCallback((message, replyCode, replyText, exchange, routingKey) -> {
      if (logger.isDebugEnabled()) {
        logger.debug(
            message.getMessageProperties().getCorrelationId() + " message return failed routingKey:"
                + routingKey + ",exchange:" + exchange + ",replyCode:" + replyCode + ",replyText:"
                + replyText);
      }
      RabbitService.logFailed(new MessageInfo(exchange, routingKey, message));
    });
    template.setRetryTemplate(rabbitRetryTemplate);
    template.setRecoveryCallback(new RabbitRecoveryCallback());
    return template;
  }


  @Autowired
  public RabbitAutoConfiguration(RabbitProperties rabbitProperties) {
    this.rabbitProperties = rabbitProperties;
  }

  @Bean
  @ConditionalOnMissingBean(name = "rabbitRetryTemplate", value = RetryTemplate.class)
  public RetryTemplate rabbitRetryTemplate() {
    RetryTemplate template = new RetryTemplate();
    CircuitBreakerRetryPolicy policy = new CircuitBreakerRetryPolicy(new SimpleRetryPolicy(3));
    policy.setOpenTimeout(rabbitProperties.getRetryOpenTimeout());
    policy.setResetTimeout(rabbitProperties.getRetryResetTimeout());
    template.setRetryPolicy(policy);
    return template;
  }
}
