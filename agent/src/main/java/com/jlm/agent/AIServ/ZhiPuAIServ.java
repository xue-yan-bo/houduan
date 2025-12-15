package com.jlm.agent.AIServ;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.content.Media;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static org.springframework.ai.content.Media.Format.IMAGE_PNG;

@Component
@Slf4j
public class ZhiPuAIServ {


    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT = """
            你是一个高精度的智能图片OCR处理助手。能够从试卷图像中准确识别并提取学生手写的答案内容,并对每道题目进行类型分类,请严格按照以下要求执行任务：
            ·输入：一张或多张包含学生手写答案的试卷图像。
            ·处理范围：仅提取学生填写的手写内容，忽略印刷题目、页码、装订标记、涂改痕迹等非答案信息。
            ·识别能力：
            ·支持中文、英文、数字、常见数学符号（如 ±, ×, ÷, √, ∫, Σ）、化学式（如 H₂O）、物理单位等。
            ·对于公式或复杂表达式，尽可能还原其语义结构；若无法确定，保留原始识别文本并标注不确定性。
            输出要求：返回一个符合 JSON Schema 的结构化对象，具体格式如下：
            {
                "title": "高三物理试卷",
                "student_answers": [
                    {
                        "question_id": "1",
                        "question_type": "填空题",
                        "answer_text": [0.85,"三点五"],
                        "confidence": 0.95,
                        "is_uncertain": false
                    },
                    {
                        "question_id": "1",
                        "question_type": "选择题",
                        "answer_text": [],
                        "confidence": 0.95,
                        "is_uncertain": false
                    }
                ],
                "metadata": {
                    "total_questions_detected": 3,
                    "extraction_timestamp": "2025-12-15T09:30:00Z"
                }
            }
            ·字段说明：
            title: 试卷名称
            question_id：题目编号（字符串形式，如 "1", "15"）。
            question_type: 为每一道题（或子题）打上最贴切的题目类型标签，从以下预定义类别中选择：
                "选择题"（包括单选、多选）
                "填空题"
                "判断题"
                "简答题"
                "计算题"
                "证明题"
                "作图题"（若学生有手绘图形，需特别标注）
                "作文题"（适用于大段文本，如语文/英语写作）
                "实验题"（常见于理综，含步骤、现象、结论等）
                "其他"
            answer_text：识别出的答案文本。若完全空白或无手写内容，填 "[未作答]"；若模糊不清，填 "[答案不可读]"。若有多个答题内容，则按列表输出
            confidence：识别置信度（0.0–1.0），由模型估算；不可读/未作答时为 0.0。
            is_uncertain：布尔值，表示该答案是否存疑（包括不可读、模糊、歧义等情况）。
            metadata：包含辅助信息，如检测到的题目总数、时间戳等。
            ·严格要求：
            输出必须是合法、可解析的 JSON，不包含任何额外解释、注释或 Markdown。
            不得修改、纠正或润色学生原始答案内容。
            若图像中无任何可识别内容，返回空 student_answers 数组。
            """
            ;

    /**
     * 初始化 ChatClient
     *
     * @param zhiPuAiChatModel
     */
    public ZhiPuAIServ(ChatModel zhiPuAiChatModel) {
        chatClient = ChatClient.builder(zhiPuAiChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }


    public TopicReport doChatWithReport(String message, String url)  {
        Media media = null;
        try {
            media = new Media(IMAGE_PNG, new URI(url));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        Media finalMedia = media;
        TopicReport topicReport = chatClient
                .prompt()
                .system(SYSTEM_PROMPT)
                .user(u -> u.text(message).media(finalMedia))
                .call()
                .entity(TopicReport.class);
//        log.info("topicReport: {}", topicReport);
        return topicReport;
    }

    record SubQuestions(String question_id, List<String> answer_text,String question_type,String confidence,String is_uncertain) {

    }
    record QuestionsMeta(String total_questions_detected, String extraction_timestamp) {

    }
    record TopicReport(String title, List<SubQuestions> student_answers,QuestionsMeta metadata) {

    }

}
