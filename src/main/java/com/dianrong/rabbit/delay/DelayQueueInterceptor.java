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

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * @author liushiming
 * @version DelayQueueINterceptor.java, v 0.0.1 2017年6月23日 下午12:55:20 liushiming
 * @since JDK 1.8
 */
public class DelayQueueInterceptor implements MethodInterceptor {

  private final RabbitTemplate rabbitTemplate;


  public DelayQueueInterceptor(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  @Override
  public Object invoke(MethodInvocation arg) throws Throwable {
    Delay delayAnnotation = arg.getMethod().getAnnotation(Delay.class);
    if (delayAnnotation != null) {
      setDelay(delayAnnotation.queue(), delayAnnotation.interval());
      return arg.proceed();
    } else {
      return arg.proceed();
    }
  }

  private void setDelay(String queueName, Long interval) {
    if (rabbitTemplate instanceof RabbitTemplateExtend) {
      RabbitTemplateExtend rabbitTemplateWrap = (RabbitTemplateExtend) rabbitTemplate;
      rabbitTemplateWrap.setDelayQueue(queueName, interval);
    }
  }

}
