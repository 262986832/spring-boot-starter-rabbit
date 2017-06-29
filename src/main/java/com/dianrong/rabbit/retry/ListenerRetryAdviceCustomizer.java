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

import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.NewMessageIdentifier;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties.ListenerRetry;
import org.springframework.classify.BinaryExceptionClassifier;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.interceptor.StatefulRetryOperationsInterceptor;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 * @author liushiming
 * @version ListenerRetryAdviceCustomizer.java, v 0.0.1 2017年6月26日 下午1:13:50 liushiming
 * @since JDK 1.8
 */
public class ListenerRetryAdviceCustomizer implements InitializingBean {

  private final SimpleRabbitListenerContainerFactory containerFactory;

  private final RabbitProperties rabbitPropeties;

  private final RabbitTemplate rabbitTemplate;

  private AmqpAdmin amqpAdmin;

  public ListenerRetryAdviceCustomizer(SimpleRabbitListenerContainerFactory containerFactory,
      RabbitProperties rabbitPropeties, RabbitTemplate rabbitTemplate, AmqpAdmin amqpAdmin) {
    this.containerFactory = containerFactory;
    this.rabbitPropeties = rabbitPropeties;
    this.rabbitTemplate = rabbitTemplate;
    this.amqpAdmin = amqpAdmin;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    ListenerRetry retryConfig = rabbitPropeties.getListener().getRetry();
    if (retryConfig.isEnabled()) {
      MethodInterceptor methodInterceptor = this.buildInterceptor(retryConfig);
      if (methodInterceptor instanceof StatefulRetryOperationsInterceptor) {
        BinaryExceptionClassifier retryableClassifier = new BinaryExceptionClassifier(false);
        ((StatefulRetryOperationsInterceptor) methodInterceptor)
            .setRollbackClassifier(retryableClassifier);
      }
      containerFactory.setAdviceChain(methodInterceptor);
    }
  }

  private MethodInterceptor buildInterceptor(ListenerRetry retryConfig) {
    RetryInterceptorBuilder<?> interceptorBuilder =
        RetryInterceptorBuilder.stateful().newMessageIdentifier(buildNewMessageIdentifier());
    RetryTemplate retryTemplate = this.buildRetryTemplate();
    MessageRecoverer messageRecoverer =
        this.buildMessageRecoverer(retryConfig.getMaxAttempts(), retryConfig.getInitialInterval());
    interceptorBuilder.retryOperations(retryTemplate).recoverer(messageRecoverer);
    return interceptorBuilder.build();
  }

  private MessageRecoverer buildMessageRecoverer(int recoverTimes, Long interval) {
    RepublishDeadLetterRecoverer recoverer =
        new RepublishDeadLetterRecoverer(rabbitTemplate, amqpAdmin);
    recoverer.setRecoverTimes(recoverTimes);
    recoverer.setInterval(interval);
    return recoverer;
  }

  private RetryTemplate buildRetryTemplate() {
    RetryTemplate retryTemplate = new RetryTemplate();
    RetryPolicy retryPolicy = new SimpleRetryPolicy(1);
    retryTemplate.setRetryPolicy(retryPolicy);
    retryTemplate.setThrowLastExceptionOnExhausted(false);
    return retryTemplate;
  }


  private NewMessageIdentifier buildNewMessageIdentifier() {
    return new NewMessageIdentifier() {

      @Override
      public boolean isNew(Message message) {
        return true;
      }
    };
  }

}
