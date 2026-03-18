package com.jlm.homework.service.impl;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.alibaba.nacos.shaded.com.google.gson.JsonArray;
import com.jlm.agent.domain.SubQuestionsEnt;
import com.jlm.homework.dto.*;
import com.jlm.homework.entity.AiCallback;
import com.jlm.homework.entity.QuestionAnalysis;
import com.jlm.homework.entity.WrongTitleBook;
import com.jlm.homework.repository.AiCallbackRepository;
import com.jlm.homework.service.IAiCallbackService;
import com.jlm.homework.service.IClassroomExercisesStudentRecordService;
import com.jlm.homework.service.IStudentsHomeworkNewService;
import com.jlm.homework.util.HttpUtil;
import com.tencentcloudapi.lke.v20231130.models.AICallConfig;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
@Slf4j
@Service
public class AiCallbackServiceImpl implements IAiCallbackService {

    @Resource
    private AiCallbackRepository aiCallbackRepository;
    @Resource
    private IStudentsHomeworkNewService studentsHomeworkNewService;
    @Resource
    private IClassroomExercisesStudentRecordService classroomExercisesStudentRecordService;
    @Override
    public AiCallbackResult aicallback(AiCallbackRequest aiCallbackRequest) {
        log.info("回调入参" + JSON.toJSONString(aiCallbackRequest));
        AiCallbackResult result = new AiCallbackResult();
        AiCallback aiCallback = new AiCallback();

        if (aiCallbackRequest == null) {
            result.setStatus(1);
            result.setMsg("回调参数为空");
            result.setDealTime(new Date());
            aiCallback.setCreateTime(new Date());
            aiCallback.setMsg(result.getMsg());
            aiCallback.setStatus(result.getStatus());
            aiCallback.setDealTime(result.getDealTime());
            aiCallbackRepository.save(aiCallback);
            return result;
        }

        result.setAiTaskId(aiCallbackRequest.getAiTaskId());
        result.setBusinessId(aiCallbackRequest.getBusinessId());
        BeanUtils.copyProperties(aiCallbackRequest, aiCallback);
        aiCallback.setCreateTime(new Date());
        aiCallback.setAiResult(JSON.toJSONString(aiCallbackRequest.getAiResult()));
        aiCallbackRepository.save(aiCallback);

        try {
            Object aiResult = aiCallbackRequest.getAiResult();
            if (aiResult == null) {
                result.setStatus(2);
                result.setMsg("AI结果为空");
            } else if (StringUtils.isEmpty(aiCallbackRequest.getBusinessId())) {
                result.setStatus(1);
                result.setMsg("业务ID为空");
            } else {
                Long businessId;
                try {
                    businessId = Long.parseLong(aiCallbackRequest.getBusinessId());
                } catch (NumberFormatException e) {
                    result.setStatus(1);
                    result.setMsg("业务ID格式错误: " + aiCallbackRequest.getBusinessId());
                    businessId = null;
                }

                if (businessId != null) {
                    List<SubQuestionsEnt> answers = extractAnswers(aiResult);
                    if (CollectionUtils.isEmpty(answers)) {
                        result.setStatus(2);
                        result.setMsg("AI解析错误");
                    } else if ("作业批改".equals(aiCallbackRequest.getBusinessType())) {
                        studentsHomeworkNewService.aiResultDeal(businessId, answers);
                        result.setStatus(0);
                        result.setMsg("OK");
                    } else {
                        classroomExercisesStudentRecordService.aiResultDeal(businessId, answers);
                        result.setStatus(0);
                        result.setMsg("OK");
                    }
                }
            }
        } catch (Exception e) {
            log.info(e.getMessage());
            result.setStatus(1);
            result.setMsg(e.getMessage());
        }
        result.setDealTime(new Date());
        aiCallback.setMsg(result.getMsg());
        aiCallback.setStatus(result.getStatus());
        aiCallback.setDealTime(result.getDealTime());
        aiCallbackRepository.save(aiCallback);
        return result;
    }

    private List<SubQuestionsEnt> extractAnswers(Object aiResult) {
        List<SubQuestionsEnt> answers = new ArrayList<>();
        if (aiResult == null) {
            return answers;
        }
        try {
            JSONObject jsonObject = JSONObject.parseObject(JSON.toJSONString(aiResult));
            if (jsonObject.containsKey("answers")) {
                List<SubQuestionsEnt> list = jsonObject.getList("answers", SubQuestionsEnt.class);
                if (list != null) {
                    answers.addAll(list);
                }
            } else if (jsonObject.containsKey("results")) {
                JSONArray jsonArray = jsonObject.getJSONArray("results");
                if (jsonArray != null) {
                    for (int i = 0; i < jsonArray.size(); i++) {
                        JSONObject jsonObject1 = jsonArray.getJSONObject(i);
                        if (jsonObject1.containsKey("answers")) {
                            List<SubQuestionsEnt> list = jsonObject1.getList("answers", SubQuestionsEnt.class);
                            if (list != null) {
                                answers.addAll(list);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.info("AI回调解析失败: {}", e.getMessage());
        }
        return answers;
    }


}
