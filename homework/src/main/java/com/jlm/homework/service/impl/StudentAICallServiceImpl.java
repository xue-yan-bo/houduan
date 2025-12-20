package com.jlm.homework.service.impl;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
//import com.jlm.agent.AIServ.TongYiAIAgent;
import com.jlm.agent.AIServ.TongYiSDKServ;
import com.jlm.agent.AIServ.ZhiPuAIAgent;
import com.jlm.agent.domain.TopicReportEnt;
import com.jlm.homework.config.ZhipuAIConfig;
import com.jlm.homework.dto.*;
import com.jlm.homework.entity.*;
import com.jlm.homework.feign.SchoolFeignClient;
import com.jlm.homework.feign.StudentFeignClient;
import com.jlm.homework.repository.*;
import com.jlm.homework.service.*;
import com.jlm.homework.util.*;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.content.Media;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

@Slf4j
@Service
public class StudentAICallServiceImpl implements IStudentAICallService {

    @Resource
    private ZhiPuAIAgent zhiPuAIAgent;
//    @Resource
//    private TongYiAIAgent tongYiAIAgent;
    @Autowired
    private AIUtil aiUtil;
    @Resource
    private TongYiSDKServ tongYiSDKServ;



    private static final String OBTAIN_STUDENTANSWER_SYSTEM_PROMPT = """
            你是一个高精度的智能图片OCR处理助手。能够从试卷图像中准确识别并提取学生手写的答案内容,并对每道题目进行类型分类,请严格遵循以下指令处理所有上传的图片，对用户上传的一张或多张试卷图像进行完整、准确、无遗漏的分析与结构化输出：
            ·输入说明：
                一张或多张包含学生手写答案的试卷图像。
                所有图像共同构成一份完整的答题内容，需跨图合并相同题号的答案，并确保每道题仅出现一次
            ·处理范围：仅提取学生填写的手写内容，忽略印刷题目、页码、装订标记、涂改痕迹等非答案信息。
            ·识别能力：
            ·支持中文、英文、数字、常见数学符号（如 ±, ×, ÷, √, ∫, Σ）、化学式（如 H₂O）、物理单位等。
            ·对于公式或复杂表达式，尽可能还原其语义结构；若无法确定，保留原始识别文本并标注不确定性。
            输出要求：返回一个符合 JSON Schema 的结构化对象，具体格式如下：
            {
                "title": "高三物理试卷",
                "answers": [
                    {
                        "major_question_id": "一",
                        "question_id": "1",
                        "question_type": "填空题",
                        "answer_text": [0.85,"三点五"],
                        "knowledge_points": ["匀变速直线运动", "速度-时间关系"],
                    },
                    {
                        "major_question_id": "二",
                        "question_id": "1",
                        "question_type": "选择题",
                        "answer_text": [],
                        "knowledge_points": ["牛顿第一定律", "惯性概念"],
                    }
                ],
                "metadata": {
                    "total_questions_detected": 3
                }
            }
            ·字段说明：
            title: 试卷名称
            major_question_id：所属大题编号（字符串格式，如 "21"、"Ⅱ"、"三"）。若题目无明确大题结构，可与 question_id 相同或设为 "独立题"
            question_id：题目编号（字符串形式，如 "1", "15"）。相同题号的内容必须合并为一条记录（例如：第1题有多个填空，则所有答案合并到同一个 question_id: "1" 条目中）。
            question_type: 为每一道题（或子题）打上最贴切的题目类型标签，从以下预定义类别中选择：
                "选择题"（包括单选、多选）
                "填空题"
                "判断题" (答案必须为中文“正确”或“错误”)
                "简答题"
                "计算题"
                "证明题"
                "作图题"（若学生有手绘图形，需特别标注）
                "作文题"（适用于大段文本，如语文/英语写作）
                "实验题"（常见于理综，含步骤、现象、结论等）
                "其他"
            answer_text：识别出的答案文本。若完全空白或无手写内容，填 "[未作答]"；若模糊不清，填 "[答案不可读]"。答题内容按列表输出, 若为判断题，必须输出 ["正确"] 或 ["错误"]，不得使用“√/×”、“T/F”、英文或其他形式；
            knowledge_points：本题考查的知识点列表（如 ["动能定理", "功的计算"]）。基于题目内容合理推断，使用标准学科术语，若无法判断则填 []
            metadata：包含辅助信息，如检测到的题目总数、时间戳等。
            ·严格要求：
            必须处理所有上传的图片,不得跳过任何一张,不得遗漏任何一张图中的题目,即使答案为空。
            输出必须是合法、可解析的 JSON，不包含任何额外解释、注释或 Markdown
            不得修改、纠正或润色学生原始答案内容。除非是将判断题的非标准表示（如“√”、“对”、“T”等）统一转换为“正确”或“错误”
            若图像中无任何可识别内容，返回空 answers 数组。
            输出前进行JSON合法性校验，去掉不合法的内容
            """
            ;


    private static final String OBTAIN_TEACHERANSWER_SYSTEM_PROMPT = """
            你是一位经验丰富的学科教师，现需对上传的试卷图像进行逐题解析与结构化作答。请严格遵循以下指令处理所有输入内容，并输出符合指定 JSON Schema 的结果。
            ·输入：一张或多张包含学生手写答案的试卷图像。
                所有图像共同构成一份完整的答题内容，需跨图合并相同题号的答案，并确保每道题仅出现一次
            ·处理范围：必须覆盖所有图片中出现的所有题目，不得遗漏任何可识别内容。若图像中无任何可识别文字或题目，返回空的 answers 数组
            输出要求：返回一个符合 JSON Schema 的结构化对象，具体格式如下：
            {
                "title": "高三物理试卷",
                "answers": [
                    {
                        "major_question_id": "一",
                        "question_id": "1",
                        "question_type": "填空题",
                        "correct_answer": ["6 m/s"],
                        "question_content": "物体做匀加速直线运动，初速度为0，加速度为2 m/s²，求3秒末的速度。",
                        "knowledge_points": ["匀变速直线运动", "速度-时间关系"],
                    },
                    {
                        "major_question_id": "二",
                        "question_id": "1",
                        "question_type": "选择题",
                        "correct_answer": ["错误"],
                        "question_content": "物体做匀加速直线运动，初速度为0，加速度为2 m/s²，求3秒末的速度。",
                        "knowledge_points": ["牛顿第一定律", "惯性概念"],
                    }
                ],
                "metadata": {
                    "total_questions_detected": 3
                }
            }
            ·字段说明：
            title: 试卷名称
            major_question_id：所属大题编号（字符串格式，如 "21"、"Ⅱ"、"三"）。若题目无明确大题结构，可与 question_id 相同或设为 "独立题"
            question_id：题目编号（字符串形式，如 "1", "15"）。相同题号的内容必须合并为一条记录（例如：第1题有多个填空，则所有答案合并到同一个 question_id: "1" 条目中）。
            question_type: 为每一道题（或子题）打上最贴切的题目类型标签，从以下预定义类别中选择：
                "选择题"（包括单选、多选）
                "填空题"
                "判断题" (答案必须为true或false)
                "简答题"
                "计算题"
                "证明题"
                "作图题"（若学生有手绘图形，需特别标注）
                "作文题"（适用于大段文本，如语文/英语写作）
                "实验题"（常见于理综，含步骤、现象、结论等）
                "其他"
            correct_answer:  该题的标准答案或参考答案（由系统基于题目内容推理得出；若无法确定，填 "[标准答案未知]"）,答题内容按列表输出, 若为判断题，必须输出 ["正确"] 或 ["错误"]，不得使用“√/×”、“T/F”、英文或其他形式；
            question_content：题目原文内容（尽可能完整转录，保留关键信息；若无法识别，填 "[题目不可读]"
            knowledge_points：本题考查的知识点列表（如 ["动能定理", "功的计算"]）。基于题目内容合理推断，使用标准学科术语，若无法判断则填 []
            metadata：包含辅助信息，如检测到的题目总数。
            ·严格要求：
            完整性：必须处理所有上传图片中的每一个可识别题目。
            合法性：输出必须是合法 JSON，符合上述 Schema。
            转义规范：若答案或知识点中包含换行符、引号、制表符等特殊字符，必须按 JSON 标准进行转义。
            一致性：相同 question_id 的内容必须合并，不得拆分为多条记录。
            类型准确性：question_type 必须从预定义列表中选择，不得自创类别。
            """
            ;

    private static final String bak1 = """
            你是一个高精度的智能试卷分析助手，能够从学生手写试卷图像中准确识别题目内容与作答信息。能够从试卷图像中准确识别并提取学生手写的答案内容,并对每道题目进行类型分类,请严格遵循以下指令处理所有上传的图片，确保每张图中出现的所有题目均被完整识别与结构化输出：
            ·输入：一张或多张包含学生手写答案的试卷图像。
                所有图像共同构成一份完整的答题内容，需跨图合并相同题号的答案，并确保每道题仅出现一次
            ·处理范围：所有图片中的所有题目。
            ·识别能力：
            ·支持中文、英文、数字、常见数学符号（如 ±, ×, ÷, √, ∫, Σ）、化学式（如 H₂O）、物理单位等。
            ·对于公式或复杂表达式，尽可能还原其语义结构；若无法确定，保留原始识别文本并标注不确定性。
            ·基于题目标准答案或逻辑规则，对学生的手写答案进行自动对错判断（需结合题目类型与学科知识）
            输出要求：返回一个符合 JSON Schema 的结构化对象，具体格式如下：
            {
                "title": "高三物理试卷",
                "answers": [
                    {
                        "major_question_id": "一",
                        "question_id": "1",
                        "question_type": "填空题",
                        "answer_text": [0.85,"三点五"],
                        "question_content": "物体做匀加速直线运动，初速度为0，加速度为2 m/s²，求3秒末的速度。",
                        "knowledge_points": ["匀变速直线运动", "速度-时间关系"],
                        "correct_answer": "6 m/s",
                        "is_correct": true,
                        "feedback": "选项正确。"
                    },
                    {
                        "major_question_id": "二",
                        "question_id": "1",
                        "question_type": "选择题",
                        "answer_text": [],
                        "question_content": "下列说法正确的是：A. ... B. ... C. ... D. ...",
                        "knowledge_points": ["牛顿第一定律", "惯性概念"],
                        "correct_answer": "错误",
                        "is_correct": true,
                        "feedback": "选项正确。"
                    }
                ],
                "metadata": {
                    "total_questions_detected": 3
                }
            }
            ·字段说明：
            title: 试卷名称
            major_question_id：所属大题编号（字符串格式，如 "21"、"Ⅱ"、"三"）。若题目无明确大题结构，可与 question_id 相同或设为 "独立题"
            question_id：题目编号（字符串形式，如 "1", "15"）。相同题号的内容必须合并为一条记录（例如：第1题有多个填空，则所有答案合并到同一个 question_id: "1" 条目中）。
            question_type: 为每一道题（或子题）打上最贴切的题目类型标签，从以下预定义类别中选择：
                "选择题"（包括单选、多选）
                "填空题"
                "判断题" (答案必须为“true”或“false”)
                "简答题"
                "计算题"
                "证明题"
                "作图题"（若学生有手绘图形，需特别标注）
                "作文题"（适用于大段文本，如语文/英语写作）
                "实验题"（常见于理综，含步骤、现象、结论等）
                "其他"
            answer_text：识别出的答案文本。若完全空白或无手写内容，填 "[未作答]"；若模糊不清，填 "[答案不可读]"。若有多个答题内容，则按列表输出, 若为判断题，必须输出 ["正确"] 或 ["错误"]，不得使用“√/×”、“T/F”、英文或其他形式；
            knowledge_points：本题考查的知识点列表（如 ["动能定理", "功的计算"]）。基于题目内容合理推断，使用标准学科术语，若无法判断则填 []
            question_content：题目原文内容（尽可能完整转录，保留关键信息；若无法识别，填 "[题目不可读]"
            metadata：包含辅助信息，如检测到的题目总数、时间戳等。
            correct_answer:  该题的标准答案或参考答案（由系统基于题目内容推理得出；若无法确定，填 "[标准答案未知]"）。
            is_correct:  比较answer_text和correct_answer，判断学生答案是否正确：
                可明确判断 → true / false
                无法判断（如简答、作文、作图）→ null
            feedback: 简要评语（如“数值正确但缺单位”、“概念混淆”、“答案合理但表述不完整”等；若无法评分，填 "[需人工阅卷]"）。
            ·严格要求：
            必须处理所有上传的图片，不得遗漏任何一张图中的题目
            输出必须是合法、可解析的 JSON，不包含任何额外解释、注释或 Markdown。
            不得修改、纠正或润色学生原始答案内容。除非是将判断题的非标准表示（如“√”、“对”、“T”等）统一转换为“正确”或“错误”
            若图像中无任何可识别内容，返回空 answers 数组。
            """
            ;

    private static String OBTAIN_TEACHERJUDGE_SYSTEM_PROMPT_QianWen = """
            你是一个高精度试卷分析助手，**执行以下规则时必须100%原样输出**，不得修改任何字符：
            
            【核心原则】
            1. **原样输出（绝对优先）**：
               - `answer_text` 和 `question_content` 必须是OCR识别的**原始文本**，**禁止**添加、删除、修改或润色任何字符。
               - 仅允许对判断题答案进行标准化：将“√”→`["正确"]`，“错”→`["错误"]`，其他非标准形式→`[答案不可读]`。
               - 例：学生写“6.0” → `["6.0"]`；写“三点五” → `["三点五"]`；写“6” → `["6"]`（不改为“6.0”）。
            2. **判断准确性（仅限客观题）**：
               - 仅当题干信息充分且答案可直接比对时，才设置 `is_correct` 为 `true`/`false`。
               - 非客观题（简答/作文/作图等）→ `is_correct: null`。
               - 未作答（`answer_text` 为空列表）→ `is_correct: false`（对选择题/填空题等）。
            
            【必须禁止的行为（违反即导致错误）】
             任何字符修改： \s
               - 例：将“6”改为“6.0” → 严格保留“6” \s
               - 例：将“三点五”改为“3.5” → 严格保留“三点五” \s
             未作答设为 `true`： \s
               - 例：选择题空答案 → `is_correct: false`（非 `true`） \s
             主观判断： \s
               - 例：简答写“重力加速度9.8” → `is_correct: null` + `feedback: "[需人工阅卷]"`
             信息不足强行推理： \s
               - 例：题干缺失单位 → `correct_answer: "[标准答案未知]"`
               - 例：题干不完整 → `correct_answer: "[标准答案未知]"`
            
            【操作细则】
            1. **答案标准化（必须执行）**：
               - 判断题： \s
                 - 输入“√” → `["正确"]` \s
                 - 输入“对” → `["正确"]` \s
                 - 输入“×” → `["错误"]` \s
                 - 输入“错” → `["错误"]` \s
                 - 其他 → `[答案不可读]`
               - 其他题型：**原样保留**（如“6.0”、“H₂O”、“三点五”）。
            2. **标准答案生成**：
               - 仅当题干信息完整时推理（例：物理题“求速度”→`correct_answer: "6 m/s"`）。
               - 信息不足 → `correct_answer: "[标准答案未知]"`
            3. **输出字段规则**：
               - `is_correct` 为 `true`/`false` 时，`feedback` 必须具体（如“单位缺失”）。
               - `is_correct` 为 `null` 时，`feedback: "[需人工阅卷]"`。
            
            【输出示例（严格遵循原样）】
            {
              "title": "高三物理试卷",
              "answers": [
                {
                  "major_question_id": "一",
                  "question_id": "1",
                  "question_type": "填空题",
                  "answer_text": ["6.0"],
                  "question_content": "物体初速0，加速度2m/s²，求3秒末速度",
                  "knowledge_points": ["匀变速直线运动"],
                  "correct_answer": "6 m/s",
                  "is_correct": false,
                  "feedback": "单位缺失"
                },
                {
                  "major_question_id": "二",
                  "question_id": "1",
                  "question_type": "判断题",
                  "answer_text": ["正确"],
                  "question_content": "物体在真空中下落速度与质量无关。",
                  "knowledge_points": ["自由落体"],
                  "correct_answer": "正确",
                  "is_correct": true,
                  "feedback": "答案正确"
                },
                {
                  "major_question_id": "三",
                  "question_id": "1",
                  "question_type": "简答题",
                  "answer_text": ["重力加速度g=9.8m/s²"],
                  "question_content": "解释重力加速度",
                  "knowledge_points": ["万有引力"],
                  "correct_answer": "[标准答案未知]",
                  "is_correct": null,
                  "feedback": "[需人工阅卷]"
                }
              ],
              "metadata": {
                "total_questions_detected": 3
              }
            }
            
            【强制要求】
            - 处理所有图片，跨图合并相同 `major_question_id + question_id`。
            - 输出**必须是纯JSON**，无任何额外文字、注释或Markdown。
            - 无识别内容 → `{"title":"未命名试卷","answers":[],"metadata":{"total_questions_detected":0}}`
            """;




    @Override
    public ZhiPuAIAgent.TopicReport obtainStudentAnswer(String userMessage, List<Media> media) {
        AIUtil util = aiUtil.getAIUtil();
        String aiName = util.getAiName();
        if("qianwen".equals(aiName)){
            log.debug("AI--使用--通义千问----------");
//            tongYiAIAgent.multiCall(OBTAIN_STUDENTANSWER_SYSTEM_PROMPT, userMessage, media);
        }
        return zhiPuAIAgent.multiCall(OBTAIN_STUDENTANSWER_SYSTEM_PROMPT, userMessage, media);
    }

    @Override
    public TopicReportEnt obtainStudentAnswerNoStruc(String userMessage, List<Media> media) {
        String aiName = aiUtil.getAiName();
        if("qianwen".equals(aiName)){
            log.debug("AI--使用--通义千问----------");
//            tongYiAIAgent.multiCallNoStruc(OBTAIN_STUDENTANSWER_SYSTEM_PROMPT, userMessage, media);
        }
        return zhiPuAIAgent.multiCallNoStruc(OBTAIN_STUDENTANSWER_SYSTEM_PROMPT, userMessage, media);
    }

    @Override
    public ZhiPuAIAgent.TopicReport obtainTeacherAnswer(String userMessage, List<Media> media) {
        String aiName = aiUtil.getAiName();
        if("qianwen".equals(aiName)){
            log.debug("AI--使用--通义千问----------");
//            tongYiAIAgent.multiCall(OBTAIN_TEACHERANSWER_SYSTEM_PROMPT, userMessage, media);
        }
        return zhiPuAIAgent.multiCall(OBTAIN_TEACHERANSWER_SYSTEM_PROMPT, userMessage, media);
    }

    public TopicReportEnt obtainTeacherAnswerNoStruc(String userMessage, List<Media> media){
        String aiName = aiUtil.getAiName();
        if("qianwen".equals(aiName)){
            log.debug("AI--使用--通义千问----------");
            return tongYiSDKServ.multiModalCall(OBTAIN_TEACHERANSWER_SYSTEM_PROMPT,userMessage,transMediaToStr(media));
//            tongYiAIAgent.multiCallNoStruc(OBTAIN_TEACHERANSWER_SYSTEM_PROMPT, userMessage, media);
        }
        return zhiPuAIAgent.multiCallNoStruc(OBTAIN_TEACHERANSWER_SYSTEM_PROMPT, userMessage, media);
    }


    @Override
    public ZhiPuAIAgent.TopicJudgeReport obtainTeacherJudgeAnswer(String userMessage, List<Media> media) {
        String aiName = aiUtil.getAiName();
        if("qianwen".equals(aiName)){
            log.debug("AI--使用--通义千问----------");
//            tongYiAIAgent.multiJudgeCall(OBTAIN_TEACHERJUDGE_SYSTEM_PROMPT_QianWen, userMessage, media);
        }
        return zhiPuAIAgent.multiJudgeCall(OBTAIN_TEACHERJUDGE_SYSTEM_PROMPT_QianWen, userMessage, media);
    }

    @Override
    public TopicReportEnt obtainTeacherJudgeAnswerNoStruc(String userMessage, List<Media> media) {

        AIUtil util = aiUtil.getAIUtil();
        String aiName = util.getAiName();

//        String aiName = aiUtil.getAiName();
        if("qianwen".equals(aiName)){
            log.debug("AI--使用--通义千问----------");
//            tongYiAIAgent.multiCallNoStruc(OBTAIN_TEACHERJUDGE_SYSTEM_PROMPT, userMessage, media);
           return tongYiSDKServ.multiModalCall(OBTAIN_TEACHERJUDGE_SYSTEM_PROMPT_QianWen,userMessage,transMediaToStr(media));
        }
        return zhiPuAIAgent.multiCallNoStruc(OBTAIN_TEACHERJUDGE_SYSTEM_PROMPT_QianWen, userMessage, media);

    }

    private List<String> transMediaToStr(List<Media> medias){

        List<String> retList = new ArrayList<>();
        for(Media media : medias){
            MimeType mimeType = media.getMimeType();
            String data = media.getData().toString();
            retList.add("data:"+mimeType + ";base64," + data);
        }
        return retList;
    }







}
