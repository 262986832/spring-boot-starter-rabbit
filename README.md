# 概述

* spring-boot-starter-rabbit是针对spring boot的rabbit进行重写的一个starter插件

# 功能

*  延迟队列，针对某一些消息需要在一定的间隔才能推送给消费端的需求
*  消息重试的功能，消息重试是指由于消费端消费失败，会自动进行重试
*  消息消费失败错误拦截


# 使用

* 延迟队列
在发送端的方法体上加上 @Delay的注解

```
  @Delay
  public void send() {
    this.rabbitTemplate.convertAndSend("testexchange", "testroute", "hello");
  }

```

* 消息重试
yml添加如下配置参数

```
 retry:
        enabled: true
        maxAttempts: 3  
```
* 错误拦截

```
 @RabbitErrorHandler
  public void errorHandler(Message message, Throwable th) {
    th.printStackTrace();
  }

```

具体使用示例请详细看单元测试用例

# 原理说明

由于延迟队列与消息重试都是通过Rabbit的死信邮箱来实现的，所有延迟消息及消息重试消息都将发送到死信邮箱，队列需要有以下三个参数

```
x-message-ttl:	1000
x-dead-letter-exchange:	testexchange
x-dead-letter-routing-key:	testroute
```

所以存在一个绑定队列的问题，该jar最好配合sghub的服务治理控制台来使用

# 详细说明

###  延迟队列

  在创建队列 queueName时，如果指定了该队列需要被延迟，在RouteKey绑定阶段将自动创建一个queueName_delay的延迟队列。<br/>
   例如：创建一个队列名：tradeQueue,其绑定exchange是：tradeExchange, routeKey是:tradeRouteKey <br/>
   那将会自动创建一个与之对应的延迟队列,队列名：tradeQueue_delay,其中绑定的exchange是:DeadLetterExchange,routeKey是：tradeRouteKey <br/>
   详细有<br/>
 **********************************************
   原始队列：
   
   queueName:
   ```
    tradeQueue
   ```
   routeBinding:
   ```
     From                   RouteKey
     tradeExchange          tradeRouteKey
   ```
   <br/>
      
 **********************************************   
   延迟队列：
   
   queueName:
   ```
    tradeQueue_delay
   ```
   routeBinding:
   ```
     From                   RouteKey
     DeadLetterExchange     tradeRouteKey
   ```
   Features：
   ```
    x-message-ttl:	1000
    x-dead-letter-exchange:	tradeExchange
    x-dead-letter-routing-key:	tradeRouteKey
   ```
 
###  消费失败重试
  原理与延迟类似，都是借鉴死信邮箱
 
 与原生的spring-retry的区别：
 * spring-retry是在消费重复调用invokeListener方法，如果在重试过程中程序宕机了，该重试将不起作用
 * spring-retry会造成消息堵塞的情况 
 * 参数使用更少，只需要配置重试最大次数即可
 
 与原生的spring-retry的关系
 * 借鉴了retry机制，但是retryTemplate的retry只会一次，当消费失败后，会调用Recover处理器将消息重新Republish,由Recover处理器来计算重试次数
