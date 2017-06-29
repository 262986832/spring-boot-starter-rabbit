/*
 * Copyright 2014-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.dianrong.rabbit.retry;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;

import com.dianrong.rabbit.constant.RabbitConstant;
import com.dianrong.rabbit.deadletter.DeadLetterQueueCreator;

/**
 * @author liushiming
 * @version RepublishMessageRecovererExtend.java, v 0.0.1 2017年6月26日 下午3:19:38 liushiming
 * @since JDK 1.8
 */
public class RepublishDeadLetterRecoverer implements MessageRecoverer {

  private static final Logger logger = LoggerFactory.getLogger(RepublishDeadLetterRecoverer.class);

  private final AmqpTemplate amqpTemplate;

  private final DeadLetterQueueCreator deadLetterCreator;

  private int recoverTimes;

  private Long interval;


  public RepublishDeadLetterRecoverer(AmqpTemplate amqpTemplate, AmqpAdmin amqpAdmin) {
    this.amqpTemplate = amqpTemplate;
    this.deadLetterCreator = new DeadLetterQueueCreator(amqpAdmin);
  }

  public void setRecoverTimes(int recoverTimes) {
    this.recoverTimes = recoverTimes;
  }

  public void setInterval(Long interval) {
    this.interval = interval;
  }

  @Override
  public void recover(Message message, Throwable cause) {
    MessageProperties messageProperties = message.getMessageProperties();
    Map<String, Object> headers = message.getMessageProperties().getHeaders();
    Integer republishTimes = (Integer) headers.get(RabbitConstant.X_REPUBLISH_TIMES);
    if (republishTimes != null) {
      if (republishTimes >= recoverTimes) {
        logger
            .warn(String.format("this message [ %s] republish times >= %d times, and will discard",
                message.toString(), recoverTimes));
        return;
      } else {
        republishTimes = republishTimes + 1;
      }
    } else {
      republishTimes = 1;
    }
    headers.put(RabbitConstant.X_REPUBLISH_TIMES, republishTimes);
    messageProperties.setRedelivered(true);
    headers.put(RabbitConstant.X_EXCEPTION_STACKTRACE, getStackTraceAsString(cause));
    String retryRouteKey = genRouteKey(message);
    try {
      retryRouteKey = createRetryQueue(message);
    } finally {
      amqpTemplate.send(RabbitConstant.DEFAULT_DEADLETTEREXCHANGE_NAME, retryRouteKey, message);
      logger.info("Retry #" + republishTimes + " consumer message ["
          + message.getMessageProperties().getMessageId()
          + "] failed, and republish it to exchange ["
          + RabbitConstant.DEFAULT_DEADLETTEREXCHANGE_NAME + "] and routingKey[" + retryRouteKey
          + "]");
    }

  }


  private String createRetryQueue(Message message) {
    MessageProperties messageProperties = message.getMessageProperties();
    String exchange = messageProperties.getReceivedExchange();
    String routeKey = messageProperties.getReceivedRoutingKey();
    String queueName = messageProperties.getConsumerQueue();
    String retryQueueName = queueName + RabbitConstant.DEFAULT_RETRY_QUEUENAME_PREFIX;
    String retryRouteKey = routeKey + RabbitConstant.DEFAULT_RETRY_QUEUENAME_PREFIX;
    deadLetterCreator.createDeadLetterQueue(exchange, routeKey, retryRouteKey, queueName,
        retryQueueName, interval);
    return retryRouteKey;
  }

  private String genRouteKey(Message message) {
    return message.getMessageProperties().getReceivedRoutingKey();
  }

  private String getStackTraceAsString(Throwable cause) {
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter, true);
    cause.printStackTrace(printWriter);
    return stringWriter.getBuffer().toString();
  }

}
