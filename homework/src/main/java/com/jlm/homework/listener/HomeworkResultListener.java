package com.jlm.homework.listener;

import com.jlm.homework.service.IHomeworkResultService;
import jakarta.annotation.Resource;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 作业批改结果监听器
 * 监听 RabbitMQ 队列中的作业批改结果消息并进行异步处理
 */
@Component
public class HomeworkResultListener {
    @Autowired
    private IHomeworkResultService homeworkResultService;
    @Resource
    private RabbitTemplate rabbitTemplate;
}
