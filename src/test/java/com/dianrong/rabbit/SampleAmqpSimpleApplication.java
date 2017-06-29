package com.dianrong.rabbit;



import java.util.Date;

import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author liushiming 2017年5月16日 下午2:14:54
 * @version: SampleAmqpSimpleApplication.java, v 0.0.1 2017年5月16日 下午2:14:54 liushiming
 */
@SpringBootApplication
@RabbitListener(queues = "testqueue")
@EnableScheduling
public class SampleAmqpSimpleApplication implements CommandLineRunner {

  @Autowired
  private RabbitProperties properties;


  @Bean
  public Sender mySender() {
    return new Sender();
  }


  @RabbitHandler
  public void process(@Payload String foo) {
    System.out.println(new Date() + ": " + foo);
  }

  public static void main(String[] args) throws Exception {
    SpringApplication.run(SampleAmqpSimpleApplication.class, args);
  }


  @Override
  public void run(String... args) throws Exception {
    System.out.println(properties);

  }

}
