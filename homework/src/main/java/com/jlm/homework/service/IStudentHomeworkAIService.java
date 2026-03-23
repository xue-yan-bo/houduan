package com.jlm.homework.service;

import com.jlm.agent.domain.SubQuestionsEnt;
import com.jlm.homework.dto.AccuracyDto;
import com.jlm.homework.entity.StudentsHomeworkNew;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import java.io.IOException;
import java.util.List;

/**
 * 学生作业AI服务接口
 * 包含AI批改、AI结构化批改、AI中台批改等相关功能
 */
public interface IStudentHomeworkAIService {

    /**
     * AI批改作业
     * @param studentsHomeworkId 学生作业ID
     * @return 批改结果字符串
     */
    String aIaudit(Long studentsHomeworkId);

    /**
     * AI结构化批改作业
     * @param studentsHomeworkId 学生作业ID
     * @return 批改结果字符串
     */
    String aIauditStruc(Long studentsHomeworkId);

    /**
     * AI中台批改作业
     * @param studentsHomeworkId 学生作业ID
     * @return 批改结果字符串
     * @throws IOException IO异常
     * @throws InvalidFormatException 格式异常
     */
    String aIauditMid(Long studentsHomeworkId) throws IOException, InvalidFormatException;

    /**
     * AI批改订正作业
     * @param studentsHomeworkId 学生作业ID
     * @return 批改结果字符串
     */
    String aIauditEmend(Long studentsHomeworkId);

    /**
     * AI结构化批改订正作业
     * @param studentsHomeworkId 学生作业ID
     * @return 批改结果字符串
     */
    String aIauditEmendStruc(Long studentsHomeworkId);

    /**
     * 处理AI回调结果
     * @param studentsHomeworkId 学生作业ID
     * @param answers AI返回的答案列表
     */
    void aiResultDeal(Long studentsHomeworkId, List<SubQuestionsEnt> answers);
    /**
     * 处理AI审批（用于非中台调用的情况）
     */
    void processAIApproval(StudentsHomeworkNew studentsHomework, String type);

    void dealTongji(Long studentsHomeworkId);

    void appSubmitAI(StudentsHomeworkNew finalStudentsHomework);

    void appEmendSubmitAI(StudentsHomeworkNew finalStudentsHomework);


}
