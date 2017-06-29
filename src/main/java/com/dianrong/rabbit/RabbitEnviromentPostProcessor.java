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

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;

import com.dianrong.rabbit.retry.RepublishDeadLetterRecoverer;

/**
 * @author liushiming
 * @version RabbitEnviromentPostProcessor.java, v 0.0.1 2017年6月28日 下午2:27:46 liushiming
 * @since JDK 1.8
 */
public class RabbitEnviromentPostProcessor implements EnvironmentPostProcessor, Ordered {

  private static final Logger logger = LoggerFactory.getLogger(RepublishDeadLetterRecoverer.class);


  private static final String EXCLUDE_AUTOCONFIGURATION =
      "spring.autoconfigure.exclude[rabbitSource]";

  private static final String RABBIT_AUTOCONFIGURATION =
      "org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration";

  @Override
  public void postProcessEnvironment(ConfigurableEnvironment environment,
      SpringApplication application) {

    try {
      MutablePropertySources mutablePropertySources = environment.getPropertySources();
      Properties propertySource = new Properties();
      propertySource.setProperty(EXCLUDE_AUTOCONFIGURATION, RABBIT_AUTOCONFIGURATION);
      EnumerablePropertySource<?> enumerablePropertySource =
          new PropertiesPropertySource("rabbitSource", propertySource);
      mutablePropertySources.addFirst(enumerablePropertySource);
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
    }


  }

  @Override
  public int getOrder() {
    return 0;
  }
}
