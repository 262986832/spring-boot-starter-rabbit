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
package com.dianrong.rabbit.config;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.util.Assert;

import com.dianrong.rabbit.retry.ListenerRetryAdviceCustomizer;

/**
 * @author liushiming
 * @version SimpleRabbitListenerContainerFactoryConfigurer.java, v 0.0.1 2017年6月26日 下午7:09:47
 *          liushiming
 * @since JDK 1.8
 */
public final class SimpleRabbitListenerContainerFactoryConfigurerExtend {

  private MessageConverter messageConverter;

  private RabbitProperties rabbitProperties;

  private RabbitTemplate rabbitTemplate;

  private AmqpAdmin amqpAdmin;

  void setMessageConverter(MessageConverter messageConverter) {
    this.messageConverter = messageConverter;
  }

  void setRabbitProperties(RabbitProperties rabbitProperties) {
    this.rabbitProperties = rabbitProperties;
  }

  void setRabbitTemplate(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  void setAmqpAdmin(AmqpAdmin amqpAdmin) {
    this.amqpAdmin = amqpAdmin;
  }


  public void configure(SimpleRabbitListenerContainerFactory factory,
      ConnectionFactory connectionFactory) throws Exception {
    Assert.notNull(factory, "Factory must not be null");
    Assert.notNull(connectionFactory, "ConnectionFactory must not be null");
    factory.setConnectionFactory(connectionFactory);
    if (this.messageConverter != null) {
      factory.setMessageConverter(this.messageConverter);
    }
    RabbitProperties.Listener listenerConfig = this.rabbitProperties.getListener();
    factory.setAutoStartup(listenerConfig.isAutoStartup());
    if (listenerConfig.getAcknowledgeMode() != null) {
      factory.setAcknowledgeMode(listenerConfig.getAcknowledgeMode());
    }
    if (listenerConfig.getConcurrency() != null) {
      factory.setConcurrentConsumers(listenerConfig.getConcurrency());
    }
    if (listenerConfig.getMaxConcurrency() != null) {
      factory.setMaxConcurrentConsumers(listenerConfig.getMaxConcurrency());
    }
    if (listenerConfig.getPrefetch() != null) {
      factory.setPrefetchCount(listenerConfig.getPrefetch());
    }
    if (listenerConfig.getTransactionSize() != null) {
      factory.setTxSize(listenerConfig.getTransactionSize());
    }
    if (listenerConfig.getDefaultRequeueRejected() != null) {
      factory.setDefaultRequeueRejected(listenerConfig.getDefaultRequeueRejected());
    }
    if (listenerConfig.getIdleEventInterval() != null) {
      factory.setIdleEventInterval(listenerConfig.getIdleEventInterval());
    }
    ListenerRetryAdviceCustomizer listenerCustomizer =
        new ListenerRetryAdviceCustomizer(factory, rabbitProperties, rabbitTemplate, amqpAdmin);
    listenerCustomizer.afterPropertiesSet();
  }
}
