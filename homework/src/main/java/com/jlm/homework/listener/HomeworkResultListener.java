package com.jlm.homework.listener;

import com.jlm.homework.config.RabbitMQConfig;
import com.jlm.homework.entity.mq.HomeworkCorrectionResult;
import com.jlm.homework.service.IHomeworkResultService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 作业批改结果监听器
 * 监听 RabbitMQ 队列中的作业批改结果消息并进行异步处理
 */
@Slf4j
@Component
public class HomeworkResultListener {
    @Autowired
    private IHomeworkResultService homeworkResultService;
    @Resource
    private RabbitTemplate rabbitTemplate;
    
    /**
     * 监听作业结果队列消息
     * @param message 消息内容
     */
    @RabbitListener(queues = RabbitMQConfig.HOMEWORK_RESULT_QUEUE)
    public void listenHomeworkResultQueue(HomeworkCorrectionResult message) {
        log.info("收到作业批改结果消息 -submissionId: {},  homeworkId: {}, studentId: {}",message.getSubmissionId(),message.getHomeworkId(),message.getStudentId());
        try {
            // 处理作业结果消息
            homeworkResultService.processHomeworkResult(message);
        } catch (Exception e) {
            log.error("Failed to process homework result message: {}", e.getMessage(), e);
        }
    }
}
