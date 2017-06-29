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
package com.dianrong.rabbit.deadletter;

import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;

import com.dianrong.rabbit.constant.RabbitConstant;
import com.dianrong.rabbit.retry.RepublishDeadLetterRecoverer;
import com.google.common.collect.Maps;

/**
 * @author liushiming
 * @version DeadLetterQueueCreator.java, v 0.0.1 2017年6月29日 上午11:56:18 liushiming
 * @since JDK 1.8
 */
public class DeadLetterQueueCreator {

  private static final Logger logger = LoggerFactory.getLogger(RepublishDeadLetterRecoverer.class);

  private AmqpAdmin rabbitAdmin;

  public DeadLetterQueueCreator(AmqpAdmin rabbitAdmin) {
    this.rabbitAdmin = rabbitAdmin;
  }

  public void createDeadLetterQueue(String fromExchange, String byRouteKey, String sourceQueue,
      String delayOrRetryQueueName, Long ttl) {
    if (StringUtils.isEmpty(sourceQueue)) {
      logger.warn(
          "Have not config destination Queue, will not create delay queue by automatic，may be you must maintain binding by youself");
      return;
    }
    Properties properties = rabbitAdmin.getQueueProperties(delayOrRetryQueueName);
    if (properties == null) {
      Map<String, Object> delayQueueArgs = Maps.newHashMap();
      delayQueueArgs.put("x-message-ttl", ttl);
      delayQueueArgs.put("x-dead-letter-exchange", fromExchange);
      delayQueueArgs.put("x-dead-letter-routing-key", byRouteKey);
      Queue delayQueue = new Queue(delayOrRetryQueueName, true, false, false, delayQueueArgs);
      String returnQueueName = rabbitAdmin.declareQueue(delayQueue);
      if (returnQueueName != null) {
        Binding binding = BindingBuilder.bind(delayQueue)//
            .to(new DirectExchange(RabbitConstant.DEFAULT_DEADLETTEREXCHANGE_NAME))//
            .with(byRouteKey);//
        rabbitAdmin.declareBinding(binding);
      }
    }

  }

}
