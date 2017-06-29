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
package com.dianrong.rabbit.delay;

import java.util.Map;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.CorrelationData;

import com.dianrong.rabbit.constant.RabbitConstant;
import com.dianrong.rabbit.deadletter.DeadLetterQueueCreator;
import com.google.common.collect.Maps;
import com.rabbitmq.client.Channel;


/**
 * @author liushiming
 * @version RabbitTemplateExtend.java, v 0.0.1 2017年6月23日 上午11:54:07 liushiming
 * @since JDK 1.8
 */
public class RabbitTemplateExtend extends RabbitTemplate {

  private static final ThreadLocal<Map<String, Long>> DELAY_QUEUE_CONTENT =
      new ThreadLocal<Map<String, Long>>() {
        protected synchronized Map<String, Long> initialValue() {
          return Maps.newHashMap();
        }
      };

  private DeadLetterQueueCreator deadLetterQueueCreator;

  public void setRabbitAdmin(AmqpAdmin amqpAdmin) {
    this.deadLetterQueueCreator = new DeadLetterQueueCreator(amqpAdmin);
  }

  public RabbitTemplateExtend() {
    super();
  }

  public RabbitTemplateExtend(ConnectionFactory connectionFactory) {
    super(connectionFactory);
  }

  @Override
  protected void doSend(Channel channel, String exchange, String routingKey, Message message,
      boolean mandatory, CorrelationData correlationData) throws Exception {
    try {
      String exchangeCopy = exchange;
      Map<String, Long> delayParam = DELAY_QUEUE_CONTENT.get();
      if (delayParam != null && delayParam.size() == 1) {
        String sourceQueue = delayParam.keySet().iterator().next();
        Long interval = delayParam.get(sourceQueue);
        if (interval > 0) {
          String delayQueue = sourceQueue + RabbitConstant.DEFAULT_DELAY_QUEUENAME_PREFIX;
          deadLetterQueueCreator.createDeadLetterQueue(exchange, routingKey, sourceQueue,
              delayQueue, interval);
          exchangeCopy = RabbitConstant.DEFAULT_DEADLETTEREXCHANGE_NAME;
        }
      }
      super.doSend(channel, exchangeCopy, routingKey, message, mandatory, correlationData);
    } finally {
      DELAY_QUEUE_CONTENT.remove();
    }

  }

  public void setDelayQueue(String queueName, Long interval) {
    Map<String, Long> delayQueueParam = Maps.newHashMap();
    delayQueueParam.put(queueName, interval);
    DELAY_QUEUE_CONTENT.set(delayQueueParam);
  }

}
