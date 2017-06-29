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
package com.dianrong.rabbit;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.RabbitConnectionFactoryBean;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import com.dianrong.rabbit.config.RabbitAnnotationDrivenConfigurationExtend;
import com.dianrong.rabbit.delay.DelayQueueScaner;
import com.dianrong.rabbit.delay.RabbitTemplateExtend;
import com.rabbitmq.client.Channel;

/**
 * @author liushiming
 * @version RabbitAutoConfiguration.java, v 0.0.1 2017年6月26日 下午7:09:12 liushiming
 * @since JDK 1.8
 */
@Configuration
@ConditionalOnClass({RabbitTemplate.class, Channel.class})
@EnableConfigurationProperties(RabbitProperties.class)
public class RabbitAutoConfiguration {


  @Configuration
  @ConditionalOnMissingBean(MessageConverter.class)
  protected static class RabbitMessageConverter {
    @Bean
    public MessageConverter messageConverter() {
      return new Jackson2JsonMessageConverter();
    }
  }


  @Configuration
  @ConditionalOnMissingBean(ConnectionFactory.class)
  protected static class RabbitConnectionFactoryCreator {

    @Bean
    public CachingConnectionFactory rabbitConnectionFactory(RabbitProperties config)
        throws Exception {
      RabbitConnectionFactoryBean factory = new RabbitConnectionFactoryBean();
      if (config.determineHost() != null) {
        factory.setHost(config.determineHost());
      }
      factory.setPort(config.determinePort());
      if (config.determineUsername() != null) {
        factory.setUsername(config.determineUsername());
      }
      if (config.determinePassword() != null) {
        factory.setPassword(config.determinePassword());
      }
      if (config.determineVirtualHost() != null) {
        factory.setVirtualHost(config.determineVirtualHost());
      }
      if (config.getRequestedHeartbeat() != null) {
        factory.setRequestedHeartbeat(config.getRequestedHeartbeat());
      }
      RabbitProperties.Ssl ssl = config.getSsl();
      if (ssl.isEnabled()) {
        factory.setUseSSL(true);
        if (ssl.getAlgorithm() != null) {
          factory.setSslAlgorithm(ssl.getAlgorithm());
        }
        factory.setKeyStore(ssl.getKeyStore());
        factory.setKeyStorePassphrase(ssl.getKeyStorePassword());
        factory.setTrustStore(ssl.getTrustStore());
        factory.setTrustStorePassphrase(ssl.getTrustStorePassword());
      }
      if (config.getConnectionTimeout() != null) {
        factory.setConnectionTimeout(config.getConnectionTimeout());
      }
      factory.afterPropertiesSet();
      CachingConnectionFactory connectionFactory =
          new CachingConnectionFactory(factory.getObject());
      connectionFactory.setAddresses(config.determineAddresses());
      connectionFactory.setPublisherConfirms(config.isPublisherConfirms());
      connectionFactory.setPublisherReturns(config.isPublisherReturns());
      if (config.getCache().getChannel().getSize() != null) {
        connectionFactory.setChannelCacheSize(config.getCache().getChannel().getSize());
      }
      if (config.getCache().getConnection().getMode() != null) {
        connectionFactory.setCacheMode(config.getCache().getConnection().getMode());
      }
      if (config.getCache().getConnection().getSize() != null) {
        connectionFactory.setConnectionCacheSize(config.getCache().getConnection().getSize());
      }
      if (config.getCache().getChannel().getCheckoutTimeout() != null) {
        connectionFactory
            .setChannelCheckoutTimeout(config.getCache().getChannel().getCheckoutTimeout());
      }
      return connectionFactory;
    }

  }


  @Configuration
  @Import({RabbitConnectionFactoryCreator.class, RabbitMessageConverter.class})
  protected static class RabbitTemplateConfiguration {

    private final ObjectProvider<MessageConverter> messageConverter;

    private final RabbitProperties properties;

    public RabbitTemplateConfiguration(ObjectProvider<MessageConverter> messageConverter,
        RabbitProperties properties) {
      this.messageConverter = messageConverter;
      this.properties = properties;
    }

    @Bean
    @ConditionalOnSingleCandidate(ConnectionFactory.class)
    @ConditionalOnProperty(prefix = "spring.rabbitmq", name = "dynamic", matchIfMissing = true)
    @ConditionalOnMissingBean(AmqpAdmin.class)
    public AmqpAdmin amqpAdmin(ConnectionFactory connectionFactory) {
      return new RabbitAdmin(connectionFactory);
    }

    @Bean
    @ConditionalOnSingleCandidate(ConnectionFactory.class)
    @ConditionalOnMissingBean(RabbitTemplate.class)
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
      RabbitTemplateExtend rabbitTemplate = new RabbitTemplateExtend(connectionFactory);
      MessageConverter messageConverter = this.messageConverter.getIfUnique();
      if (messageConverter != null) {
        rabbitTemplate.setMessageConverter(messageConverter);
      }
      rabbitTemplate.setMandatory(determineMandatoryFlag());
      RabbitProperties.Template templateProperties = this.properties.getTemplate();
      RabbitProperties.Retry retryProperties = templateProperties.getRetry();
      if (retryProperties.isEnabled()) {
        rabbitTemplate.setRetryTemplate(createRetryTemplate(retryProperties));
      }
      if (templateProperties.getReceiveTimeout() != null) {
        rabbitTemplate.setReceiveTimeout(templateProperties.getReceiveTimeout());
      }
      if (templateProperties.getReplyTimeout() != null) {
        rabbitTemplate.setReplyTimeout(templateProperties.getReplyTimeout());
      }
      rabbitTemplate.setRabbitAdmin(amqpAdmin(connectionFactory));
      return rabbitTemplate;
    }

    private boolean determineMandatoryFlag() {
      Boolean mandatory = this.properties.getTemplate().getMandatory();
      return (mandatory != null ? mandatory : this.properties.isPublisherReturns());
    }

    private RetryTemplate createRetryTemplate(RabbitProperties.Retry properties) {
      RetryTemplate template = new RetryTemplate();
      SimpleRetryPolicy policy = new SimpleRetryPolicy();
      policy.setMaxAttempts(properties.getMaxAttempts());
      template.setRetryPolicy(policy);
      ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
      backOffPolicy.setInitialInterval(properties.getInitialInterval());
      backOffPolicy.setMultiplier(properties.getMultiplier());
      backOffPolicy.setMaxInterval(properties.getMaxInterval());
      template.setBackOffPolicy(backOffPolicy);
      return template;
    }


  }
  @Configuration
  @Import({RabbitTemplateConfiguration.class, RabbitAnnotationDrivenConfigurationExtend.class})
  protected static class DelayAndRetryConfiguration {

    @Bean
    @ConditionalOnSingleCandidate(RabbitTemplate.class)
    public DelayQueueScaner delayQueue(RabbitTemplate rabbitTemplate) {
      return new DelayQueueScaner(rabbitTemplate);
    }

  }

  @Configuration
  @ConditionalOnClass(RabbitMessagingTemplate.class)
  @ConditionalOnMissingBean(RabbitMessagingTemplate.class)
  @Import(RabbitTemplateConfiguration.class)
  protected static class MessagingTemplateConfiguration {

    @Bean
    @ConditionalOnSingleCandidate(RabbitTemplate.class)
    public RabbitMessagingTemplate rabbitMessagingTemplate(RabbitTemplate rabbitTemplate) {
      return new RabbitMessagingTemplate(rabbitTemplate);
    }

  }

}
