package com.jlm.homework.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String HOMEWORK_CORRECTION_QUEUE = "homework.correction.queue";

    public static final String HOMEWORK_RESULT_QUEUE = "homework.result.queue";
    public static final String HOMEWORK_RESULT_EXCHANGE = "homework.result.agentExchange";
    public static final String HOMEWORK_RESULT_ROUTING_KEY = "homework.result";
    
    // 声明作业结果队列
    @Bean
    public Queue homeworkResultQueue() {
        return new Queue(HOMEWORK_RESULT_QUEUE, true, false, false);
    }
    
    // 声明作业结果交换机
    /*@Bean
    public TopicExchange homeworkResultExchange() {
        return new TopicExchange(HOMEWORK_RESULT_EXCHANGE, true, false);
    }*/
    @Bean
    public DirectExchange homeworkResultExchange() {
        return new DirectExchange(HOMEWORK_RESULT_EXCHANGE, true, false);
    }
    // 绑定队列到交换机
    @Bean
    public Binding bindHomeworkResultQueue() {
        return BindingBuilder.bind(homeworkResultQueue())
                .to(homeworkResultExchange())
                .with(HOMEWORK_RESULT_ROUTING_KEY);
    }
}
