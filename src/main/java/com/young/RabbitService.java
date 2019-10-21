package com.young;

import com.young.core.MessageInfo;
import com.young.task.ResendTask;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

public class RabbitService implements InitializingBean {

  @Autowired
  RabbitTemplate rabbitTemplate;

  @Autowired
  RabbitProperties rabbitProperties;

  ResendTask resendTask;

  static final ConcurrentSkipListMap<String, MessageInfo> ackMap = new ConcurrentSkipListMap<>();


  public void send(String exchange, String routingKey, Message message) {
    CorrelationData correlationData = new CorrelationData();
    String uuid = UUID.randomUUID().toString();
    correlationData.setId(uuid);
    rabbitTemplate.send(exchange, routingKey, message, correlationData);
    ackMap.put(uuid, new MessageInfo(exchange, routingKey, message));
  }


  public void convertAndSend(String exchange, String routingKey, Object message) {
    CorrelationData correlationData = new CorrelationData();
    String uuid = UUID.randomUUID().toString();
    correlationData.setId(uuid);
    rabbitTemplate.convertAndSend(exchange, routingKey, message, correlationData);
    ackMap.put(uuid, new MessageInfo(exchange, routingKey, message));
  }

  public void reSend(long age) {
    Collection<CorrelationData> unconfirmed = rabbitTemplate.getUnconfirmed(age);
    if (unconfirmed==null|| unconfirmed.isEmpty()) {
      return;
    }
    unconfirmed.forEach(message -> {
      MessageInfo messageInfo = ackMap.get(message.getId());
      if (!canRetry(messageInfo)) {
        logFailed(messageInfo);
        if (RabbitAutoConfiguration.logger.isDebugEnabled()) {
          RabbitAutoConfiguration.logger.debug(
              "message retry exhausted,exchange:" + messageInfo.getExchange() + ",routingKey:"
                  + messageInfo.getRoutingKey() + ",uuid:" + message.getId());
        }
        ackMap.remove(message.getId());
      } else {
        messageInfo.setRetry(messageInfo.getRetry() + 1);
        CorrelationData correlationData = new CorrelationData();
        correlationData.setId(message.getId());
        rabbitTemplate.convertAndSend(messageInfo.getExchange(), messageInfo.getRoutingKey(),
            messageInfo.getMessage(), correlationData);
      }
    });
  }

  private boolean canRetry(MessageInfo info) {
    if (rabbitProperties.getRetryAttempts() == -1) {
      return true;
    } else {
      return info.getRetry() < rabbitProperties.getRetryAttempts();
    }
  }

  static void logFailed(MessageInfo messageInfo) {
    final Logger log = LoggerFactory.getLogger("com.young.rabbit.exhausted");
    log.warn("exchange:" + messageInfo.getExchange() + ",routingKey:"
        + messageInfo.getRoutingKey() + ",message:" + messageInfo.toString());
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    resendTask = new ResendTask(this);
  }
}
