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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.aop.Advisor;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;

import com.google.common.collect.Lists;

/**
 * @author liushiming
 * @version DelayQueueScaner.java, v 0.0.1 2017年6月23日 下午1:00:22 liushiming
 * @since JDK 1.8
 */
public class DelayQueueScaner extends AbstractAutoProxyCreator {

  private static final long serialVersionUID = 1L;

  private final Set<String> proxyedSet = new HashSet<String>();

  private final transient RabbitTemplate rabbitTemplate;

  private transient DelayQueueInterceptor interceptor;


  public DelayQueueScaner(RabbitTemplate rabbitTemplate) {
    logger.info("Delay queue scaner initing....");
    this.rabbitTemplate = rabbitTemplate;
  }

  @Override
  protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
    try {
      Object beanCopy = bean;
      synchronized (proxyedSet) {
        if (proxyedSet.contains(beanName)) {
          return beanCopy;
        }
        proxyedSet.add(beanName);
        Class<?> serviceInterface = findTargetClass(beanCopy);
        Method[] methods = serviceInterface.getMethods();
        List<Method> annotationMethods = Lists.newArrayList();
        for (Method method : methods) {
          Delay anno = method.getAnnotation(Delay.class);
          if (anno != null) {
            annotationMethods.add(method);
          }
        }
        if (!annotationMethods.isEmpty()) {
          interceptor = new DelayQueueInterceptor(rabbitTemplate);
        } else {
          return beanCopy;
        }
        if (!AopUtils.isAopProxy(beanCopy)) {
          // 未被代理过
          beanCopy = super.wrapIfNecessary(beanCopy, beanName, cacheKey);
        } else {
          // 已代理则加入当前代理
          AdvisedSupport advised = getAdvisedSupport(beanCopy);
          Advisor[] advisor =
              buildAdvisors(beanName, getAdvicesAndAdvisorsForBean(null, null, null));
          for (Advisor avr : advisor) {
            advised.addAdvisor(0, avr);
          }
        }
        // 返回被代理对象
        return beanCopy;
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected Object[] getAdvicesAndAdvisorsForBean(Class<?> beanClass, String beanName,
      TargetSource customTargetSource) throws BeansException {
    return new Object[] {interceptor};
  }


  private Class<?> findTargetClass(Object proxy) throws Exception {
    if (AopUtils.isAopProxy(proxy)) {
      AdvisedSupport advised = getAdvisedSupport(proxy);
      Object target = advised.getTargetSource().getTarget();
      return findTargetClass(target);
    } else {
      return proxy.getClass();
    }
  }

  private AdvisedSupport getAdvisedSupport(Object proxy) throws Exception {
    Field h;
    if (AopUtils.isJdkDynamicProxy(proxy)) {
      h = proxy.getClass().getSuperclass().getDeclaredField("h");
    } else {
      h = proxy.getClass().getDeclaredField("CGLIB$CALLBACK_0");
    }
    h.setAccessible(true);
    Object dynamicAdvisedInterceptor = h.get(proxy);
    Field advised = dynamicAdvisedInterceptor.getClass().getDeclaredField("advised");
    advised.setAccessible(true);
    return (AdvisedSupport) advised.get(dynamicAdvisedInterceptor);
  }


}
