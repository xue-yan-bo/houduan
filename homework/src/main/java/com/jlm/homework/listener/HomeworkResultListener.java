package com.jlm.homework.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
     * @param messageBytes 消息内容（字节数组）
     */
    @RabbitListener(queues = RabbitMQConfig.HOMEWORK_RESULT_QUEUE)
    public void listenHomeworkResultQueue(byte[] messageBytes) {
        log.info("收到作业批改结果消息，长度: {}", messageBytes.length);
        try {
            // 使用Jackson将字节数组转换为HomeworkCorrectionResult
            ObjectMapper objectMapper = new ObjectMapper();
            // 注册JavaTimeModule以支持Java 8日期时间类型
            objectMapper.registerModule(new JavaTimeModule());
            // 配置日期时间解析
            objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false);
            // 配置宽松的日期时间解析
            objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS, true);
            HomeworkCorrectionResult message = objectMapper.readValue(messageBytes, HomeworkCorrectionResult.class);
            
            log.info("解析作业批改结果消息 -submissionId: {},  homeworkId: {}, studentId: {}",
                    message.getSubmissionId(), message.getHomeworkId(), message.getStudentId());
            
            // 处理作业结果消息
            homeworkResultService.processHomeworkResult(message);
        } catch (Exception e) {
            log.error("Failed to process homework result message: {}", e.getMessage(), e);
        }
    }
}
