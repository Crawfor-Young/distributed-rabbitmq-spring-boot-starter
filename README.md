# distributed-rabbitmq-spring-boot-starter

*基于spring boot的rabbitTemplate封装

*维护了一个用于生产者检查ack的跳表保证发送方不丢消息

*配置了spring retry进行生产者发送重试

*维护了一个调度任务定期重发未收到ack的消息（幂等性需要在具体业务中保证）

*重试次数耗尽将会把发送信息输出在log4j的com.young.rabbit.exhausted的warning日志中
