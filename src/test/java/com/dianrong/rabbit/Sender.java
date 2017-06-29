package com.dianrong.rabbit;



import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import com.dianrong.rabbit.delay.Delay;

/**
 * @author liushiming 2017年5月16日 下午2:15:58
 * @version: Sender.java, v 0.0.1 2017年5月16日 下午2:15:58 liushiming
 */
public class Sender {

  @Autowired
  private RabbitTemplate rabbitTemplate;

  @Scheduled(fixedDelay = 1000L)
  @Delay
  public void send() {
    this.rabbitTemplate.convertAndSend("testexchange", "testroute", "hello");
  }

}
