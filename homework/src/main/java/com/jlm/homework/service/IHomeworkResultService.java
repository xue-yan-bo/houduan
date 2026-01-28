package com.jlm.homework.service;

import com.jlm.homework.entity.mq.HomeworkCorrectionResult;

public interface IHomeworkResultService {
    /**
     * 处理作业结果消息
     * @param message 消息内容
     */
    void processHomeworkResult(HomeworkCorrectionResult message);
}
