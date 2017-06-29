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
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.RabbitListenerConfigUtils;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author liushiming
 * @version RabbitAnnotationDrivenConfiguration.java, v 0.0.1 2017年6月26日 下午7:06:50 liushiming
 * @since JDK 1.8
 */
@Configuration
@ConditionalOnClass(EnableRabbit.class)
public class RabbitAnnotationDrivenConfigurationExtend {


  @Bean
  @ConditionalOnMissingBean(name = "rabbitListenerContainerFactory")
  public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(AmqpAdmin amqpAdmin,
      RabbitTemplate rabbitTemplate, ObjectProvider<MessageConverter> messageConverter,
      RabbitProperties rabbitProperties, ConnectionFactory connectionFactory) throws Exception {
    SimpleRabbitListenerContainerFactoryConfigurerExtend configurer =
        new SimpleRabbitListenerContainerFactoryConfigurerExtend();
    configurer.setMessageConverter(messageConverter.getIfUnique());
    configurer.setRabbitProperties(rabbitProperties);
    configurer.setRabbitTemplate(rabbitTemplate);
    configurer.setAmqpAdmin(amqpAdmin);
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    configurer.configure(factory, connectionFactory);
    return factory;
  }



  @EnableRabbit
  @ConditionalOnMissingBean(
      name = RabbitListenerConfigUtils.RABBIT_LISTENER_ANNOTATION_PROCESSOR_BEAN_NAME)
  protected static class EnableRabbitConfiguration {

  }

}
