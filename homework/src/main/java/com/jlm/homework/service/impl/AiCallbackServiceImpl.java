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
        log.info("回调入参"+JSON.toJSONString(aiCallbackRequest));
        AiCallbackResult result = new AiCallbackResult();
        result.setAiTaskId(aiCallbackRequest.getAiTaskId());
        result.setBusinessId(aiCallbackRequest.getBusinessId());
        AiCallback aiCallback = new AiCallback();
        BeanUtils.copyProperties(aiCallbackRequest, aiCallback);
        aiCallback.setCreateTime(new Date());
        aiCallback.setAiResult(JSON.toJSONString(aiCallbackRequest.getAiResult()));
        aiCallbackRepository.save(aiCallback);

        try {
            Object aiResult = aiCallbackRequest.getAiResult();
            //业务处理
            if("作业批改".equals(aiCallbackRequest.getBusinessType())){
                try {
                    JSONObject jsonObject = JSONObject.parseObject(JSON.toJSONString(aiResult));
                    //{"answers":[{"answer_text":[],"correct_answer":"22.36 二十二点三六","feedback":"未作答","is_correct":false,"knowledge_points":["小数组成"],"major_question_id":"一","question_content":"1.一个数由2个十、2个十分之一、3个百分之一和6个千分之一组成，这个数是( )，读作( )。","question_id":"1","question_score":1,"question_type":"填空题","score":0},{"answer_text":[],"correct_answer":"0.660 660","feedback":"未作答","is_correct":false,"knowledge_points":["小数改写"],"major_question_id":"一","question_content":"2. 把0.66改写成用千分之一作单位的数是( )，它有( )个0.001。","question_id":"2","question_score":1,"question_type":"填空题","score":0},{"answer_text":[],"correct_answer":"5.00 5.0","feedback":"未作答","is_correct":false,"knowledge_points":["近似数"],"major_question_id":"一","question_content":"3. 4.986精确到百分位约是( )，保留一位小数约是( )。","question_id":"3","question_score":1,"question_type":"填空题","score":0},{"answer_text":[],"correct_answer":"分配律","feedback":"","knowledge_points":["乘法分配律"],"major_question_id":"一","question_content":"4. 25×104=25×100+25×4运用了乘法( )律。","question_id":"4","question_score":1,"question_type":"填空题","score":0},{"answer_text":[],"correct_answer":"42×[(102-248÷8)]","feedback":"","knowledge_points":["运算顺序"],"major_question_id":"一","question_content":"5. 如果把算式42×102-248÷8的运算顺序改为先算除法，再算减法，最后算乘法，那么算式应改为( )。","question_id":"5","question_score":1,"question_type":"填空题","score":0},{"answer_text":[],"correct_answer":"0.426 6072 3.012 7 40","feedback":"","knowledge_points":["单位换算"],"major_question_id":"一","question_content":"6. 426克=( )千克 6.072千米=( )米 3千米12米=( )千米 7.04吨=( )吨( )千克","question_id":"6","question_score":4,"question_type":"填空题","score":0},{"answer_text":[],"correct_answer":"2.77 2.8 46.53452 46.53","feedback":"","knowledge_points":["改写数"],"major_question_id":"一","question_content":"7. 27700=( )万≈( )万(保留一位小数) 4653452000=( )亿≈( )亿(保留两位小数)","question_id":"7","question_score":1,"question_type":"填空题","score":0},{"answer_text":[],"correct_answer":"1/1000 右 2","feedback":"","knowledge_points":["小数点移动"],"major_question_id":"一","question_content":"8. 把53缩小为原来的( )是0.053；把0.07的小数点向( )移动( )位是7。","question_id":"8","question_score":1,"question_type":"填空题","score":0},{"answer_text":["√"],"correct_answer":"正确","feedback":"答案正确","is_correct":true,"knowledge_points":["运算顺序"],"major_question_id":"二","question_content":"1.35乘12与3的商，积是多少?正确列式为35×12÷3。（ ）","question_id":"1","question_score":1,"question_type":"判断题","score":1},{"answer_text":["√"],"correct_answer":"正确","feedback":"答案正确","is_correct":true,"knowledge_points":["除法性质"],"major_question_id":"二","question_content":"2.0除以0以外的任何数都得0。（ ）","question_id":"2","question_score":1,"question_type":"判断题","score":1},{"answer_text":["×"],"correct_answer":"正确","feedback":"选项不符","is_correct":false,"knowledge_points":["减法性质"],"major_question_id":"二","question_content":"3.287-(187+65)=287-187-65 （ ）","question_id":"3","question_score":1,"question_type":"判断题","score":0},{"answer_text":["√"],"correct_answer":"正确","feedback":"答案正确","is_correct":true,"knowledge_points":["小数意义"],"major_question_id":"二","question_content":"4.5.8米和5.80米所表示的长度是相等的。（ ）","question_id":"4","question_score":1,"question_type":"判断题","score":1},{"answer_text":["√"],"correct_answer":"正确","feedback":"答案正确","is_correct":true,"knowledge_points":["加法结合律"],"major_question_id":"二","question_content":"5.165+(135+192)=(165+135)+192运用了加法结合律。（ ）","question_id":"5","question_score":1,"question_type":"判断题","score":1},{"answer_text":["√"],"correct_answer":"正确","feedback":"答案正确","is_correct":true,"knowledge_points":["近似数"],"major_question_id":"二","question_content":"6.精确到千分位就是保留三位小数。（ ）","question_id":"6","question_score":1,"question_type":"判断题","score":1},{"answer_text":["③"],"correct_answer":"②","feedback":"选项不符","is_correct":false,"knowledge_points":["运算顺序影响结果"],"major_question_id":"三","question_content":"1.小军在计算60÷(4+2)时，把算式抄成60÷4+2，这样两题的计算结果相差( )。 ①8 ②7 ③5","question_id":"1","question_score":2,"question_type":"选择题","score":0},{"answer_text":["③"],"correct_answer":"③","feedback":"答案正确","is_correct":true,"knowledge_points":["小数末尾添0"],"major_question_id":"三","question_content":"2. 在一个小数的末尾添上两个0，这个小数将( )。 ①扩大到原来的100倍 ②缩小到原来的7/100 ③不变","question_id":"2","question_score":2,"question_type":"选择题","score":2},{"answer_text":["②"],"correct_answer":"①","feedback":"选项不符","is_correct":false,"knowledge_points":["近似数范围"],"major_question_id":"三","question_content":"3.1.76□≈1.76，□中可以填的数有( )个。 ①5 ②4 ③3","question_id":"3","question_score":2,"question_type":"选择题","score":0},{"answer_text":["③"],"correct_answer":"③","feedback":"答案正确","is_correct":true,"knowledge_points":["减法性质"],"major_question_id":"三","question_content":"4. 如果782-198○a=782-(198+a)，那么○里应填( )。 ①× ②+ ③－","question_id":"4","question_score":2,"question_type":"选择题","score":2},{"answer_text":["②"],"correct_answer":"③","feedback":"选项不符","is_correct":false,"knowledge_points":["单价计算"],"major_question_id":"三","question_content":"5. 学校食堂买了8套不锈钢碗，每套装9只，共花去216元钱，算式( )可用于计算每只碗的价钱。 ①216÷9×8 ②216÷8×9 ③216÷(9×8)","question_id":"5","question_score":2,"question_type":"选择题","score":0}],"metadata":{"max_total_score":35,"total_questions_detected":19,"total_score":9},"title":"人教版小学四年级数学下册期中测试卷（B）"}
                    if(jsonObject.containsKey("answers")) {
                        List<SubQuestionsEnt> answers = jsonObject.getList("answers", SubQuestionsEnt.class);
                        studentsHomeworkNewService.aiResultDeal(Long.parseLong(aiCallbackRequest.getBusinessId()), answers);
                    }else{
                        result.setStatus(2);
                        result.setMsg("AI解析错误!"+jsonObject.getString("comment"));
                    }
                } catch (NumberFormatException e) {
                    JSONObject jsonObject = JSONObject.parseObject(JSON.toJSONString(aiResult));
                    JSONArray jsonArray = jsonObject.getJSONArray("results");
                    List<SubQuestionsEnt> answers = new ArrayList<>();
                    for(int i=0;i<jsonArray.size();i++){
                        JSONObject jsonObject1 = jsonArray.getJSONObject(i);
                        if(jsonObject1.containsKey("answers")) {
                            List<SubQuestionsEnt> answerList = jsonObject1.getList("answers", SubQuestionsEnt.class);
                            answers.addAll(answerList);
                        }
                    }
                    studentsHomeworkNewService.aiResultDeal(Long.parseLong(aiCallbackRequest.getBusinessId()),answers);
                }

            }else{//随堂检测
                try {
                    JSONObject jsonObject = JSONObject.parseObject(JSON.toJSONString(aiResult));
                    //{"answers":[{"answer_text":[],"correct_answer":"22.36 二十二点三六","feedback":"未作答","is_correct":false,"knowledge_points":["小数组成"],"major_question_id":"一","question_content":"1.一个数由2个十、2个十分之一、3个百分之一和6个千分之一组成，这个数是( )，读作( )。","question_id":"1","question_score":1,"question_type":"填空题","score":0},{"answer_text":[],"correct_answer":"0.660 660","feedback":"未作答","is_correct":false,"knowledge_points":["小数改写"],"major_question_id":"一","question_content":"2. 把0.66改写成用千分之一作单位的数是( )，它有( )个0.001。","question_id":"2","question_score":1,"question_type":"填空题","score":0},{"answer_text":[],"correct_answer":"5.00 5.0","feedback":"未作答","is_correct":false,"knowledge_points":["近似数"],"major_question_id":"一","question_content":"3. 4.986精确到百分位约是( )，保留一位小数约是( )。","question_id":"3","question_score":1,"question_type":"填空题","score":0},{"answer_text":[],"correct_answer":"分配律","feedback":"","knowledge_points":["乘法分配律"],"major_question_id":"一","question_content":"4. 25×104=25×100+25×4运用了乘法( )律。","question_id":"4","question_score":1,"question_type":"填空题","score":0},{"answer_text":[],"correct_answer":"42×[(102-248÷8)]","feedback":"","knowledge_points":["运算顺序"],"major_question_id":"一","question_content":"5. 如果把算式42×102-248÷8的运算顺序改为先算除法，再算减法，最后算乘法，那么算式应改为( )。","question_id":"5","question_score":1,"question_type":"填空题","score":0},{"answer_text":[],"correct_answer":"0.426 6072 3.012 7 40","feedback":"","knowledge_points":["单位换算"],"major_question_id":"一","question_content":"6. 426克=( )千克 6.072千米=( )米 3千米12米=( )千米 7.04吨=( )吨( )千克","question_id":"6","question_score":4,"question_type":"填空题","score":0},{"answer_text":[],"correct_answer":"2.77 2.8 46.53452 46.53","feedback":"","knowledge_points":["改写数"],"major_question_id":"一","question_content":"7. 27700=( )万≈( )万(保留一位小数) 4653452000=( )亿≈( )亿(保留两位小数)","question_id":"7","question_score":1,"question_type":"填空题","score":0},{"answer_text":[],"correct_answer":"1/1000 右 2","feedback":"","knowledge_points":["小数点移动"],"major_question_id":"一","question_content":"8. 把53缩小为原来的( )是0.053；把0.07的小数点向( )移动( )位是7。","question_id":"8","question_score":1,"question_type":"填空题","score":0},{"answer_text":["√"],"correct_answer":"正确","feedback":"答案正确","is_correct":true,"knowledge_points":["运算顺序"],"major_question_id":"二","question_content":"1.35乘12与3的商，积是多少?正确列式为35×12÷3。（ ）","question_id":"1","question_score":1,"question_type":"判断题","score":1},{"answer_text":["√"],"correct_answer":"正确","feedback":"答案正确","is_correct":true,"knowledge_points":["除法性质"],"major_question_id":"二","question_content":"2.0除以0以外的任何数都得0。（ ）","question_id":"2","question_score":1,"question_type":"判断题","score":1},{"answer_text":["×"],"correct_answer":"正确","feedback":"选项不符","is_correct":false,"knowledge_points":["减法性质"],"major_question_id":"二","question_content":"3.287-(187+65)=287-187-65 （ ）","question_id":"3","question_score":1,"question_type":"判断题","score":0},{"answer_text":["√"],"correct_answer":"正确","feedback":"答案正确","is_correct":true,"knowledge_points":["小数意义"],"major_question_id":"二","question_content":"4.5.8米和5.80米所表示的长度是相等的。（ ）","question_id":"4","question_score":1,"question_type":"判断题","score":1},{"answer_text":["√"],"correct_answer":"正确","feedback":"答案正确","is_correct":true,"knowledge_points":["加法结合律"],"major_question_id":"二","question_content":"5.165+(135+192)=(165+135)+192运用了加法结合律。（ ）","question_id":"5","question_score":1,"question_type":"判断题","score":1},{"answer_text":["√"],"correct_answer":"正确","feedback":"答案正确","is_correct":true,"knowledge_points":["近似数"],"major_question_id":"二","question_content":"6.精确到千分位就是保留三位小数。（ ）","question_id":"6","question_score":1,"question_type":"判断题","score":1},{"answer_text":["③"],"correct_answer":"②","feedback":"选项不符","is_correct":false,"knowledge_points":["运算顺序影响结果"],"major_question_id":"三","question_content":"1.小军在计算60÷(4+2)时，把算式抄成60÷4+2，这样两题的计算结果相差( )。 ①8 ②7 ③5","question_id":"1","question_score":2,"question_type":"选择题","score":0},{"answer_text":["③"],"correct_answer":"③","feedback":"答案正确","is_correct":true,"knowledge_points":["小数末尾添0"],"major_question_id":"三","question_content":"2. 在一个小数的末尾添上两个0，这个小数将( )。 ①扩大到原来的100倍 ②缩小到原来的7/100 ③不变","question_id":"2","question_score":2,"question_type":"选择题","score":2},{"answer_text":["②"],"correct_answer":"①","feedback":"选项不符","is_correct":false,"knowledge_points":["近似数范围"],"major_question_id":"三","question_content":"3.1.76□≈1.76，□中可以填的数有( )个。 ①5 ②4 ③3","question_id":"3","question_score":2,"question_type":"选择题","score":0},{"answer_text":["③"],"correct_answer":"③","feedback":"答案正确","is_correct":true,"knowledge_points":["减法性质"],"major_question_id":"三","question_content":"4. 如果782-198○a=782-(198+a)，那么○里应填( )。 ①× ②+ ③－","question_id":"4","question_score":2,"question_type":"选择题","score":2},{"answer_text":["②"],"correct_answer":"③","feedback":"选项不符","is_correct":false,"knowledge_points":["单价计算"],"major_question_id":"三","question_content":"5. 学校食堂买了8套不锈钢碗，每套装9只，共花去216元钱，算式( )可用于计算每只碗的价钱。 ①216÷9×8 ②216÷8×9 ③216÷(9×8)","question_id":"5","question_score":2,"question_type":"选择题","score":0}],"metadata":{"max_total_score":35,"total_questions_detected":19,"total_score":9},"title":"人教版小学四年级数学下册期中测试卷（B）"}
                    if(jsonObject.containsKey("answers")) {
                        List<SubQuestionsEnt> answers = jsonObject.getList("answers", SubQuestionsEnt.class);
                        classroomExercisesStudentRecordService.aiResultDeal(Long.parseLong(aiCallbackRequest.getBusinessId()), answers);
                    }else{
                        result.setStatus(2);
                        result.setMsg("AI解析错误!"+jsonObject.getString("comment"));
                    }
                } catch (NumberFormatException e) {

                }
            }
            result.setStatus(0);
            result.setMsg("OK");
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




}
