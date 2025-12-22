package com.jlm.homework.service;

import com.jlm.agent.AIServ.ZhiPuAIAgent;
import com.jlm.agent.domain.TopicReportEnt;
import org.springframework.ai.content.Media;

import java.util.List;

public interface IStudentAICallService {

    /**
     * 获取学生手写答案
     */
    ZhiPuAIAgent.TopicReport obtainStudentAnswer(String userMessage, List<Media> media);
    // 因有非法字符，手工处理返回
    TopicReportEnt obtainStudentAnswerNoStruc(String userMessage, List<Media> media);

    /**
     * 获取AI作答内容
     */
    ZhiPuAIAgent.TopicReport obtainTeacherAnswer(String userMessage, List<Media> media);
    TopicReportEnt obtainTeacherAnswerNoStruc(String userMessage, List<Media> media);

    /**
     * 获取AI判卷结果
     */
    ZhiPuAIAgent.TopicJudgeReport obtainTeacherJudgeAnswer(String userMessage, List<Media> media);
    TopicReportEnt obtainTeacherJudgeAnswerNoStruc(String userMessage, List<Media> media);


}
