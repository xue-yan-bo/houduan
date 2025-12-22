package com.jlm.agent.AIServ;

import com.alibaba.fastjson2.JSON;
import com.fasterxml.jackson.databind.util.JSONPObject;
import com.jlm.agent.advisor.MyLoggerAdvisor;
import com.jlm.agent.domain.TopicReportEnt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.content.Media;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import static org.springframework.ai.content.Media.Format.IMAGE_PNG;

@Component
@Slf4j
public class ZhiPuAIAgent {

    private final ChatClient chatClient;



    /**
     * 初始化 ChatClient
     *
     * @param zhiPuAiChatModel
     */
    public ZhiPuAIAgent(ChatModel zhiPuAiChatModel) {

        chatClient = ChatClient.builder(zhiPuAiChatModel)
                .build();
    }


    public TopicReport multiCall(String SYSTEM_PROMPT, String message, String url)  {
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

    public TopicReport multiCall(String SYSTEM_PROMPT, String message, List<Media> mediaList){

        Media[] array = mediaList.toArray(new Media[0]);

        TopicReport topicReport = chatClient
                .prompt()
                .system(SYSTEM_PROMPT)
                .user(u -> u.text(message).media(array))
                .advisors(new MyLoggerAdvisor())
                .messages()
                .call()
                .entity(TopicReport.class);
//        log.info("topicReport: {}", topicReport);
        return topicReport;
    }

    public TopicReportEnt multiCallNoStruc(String SYSTEM_PROMPT, String message, List<Media> mediaList){

        Media[] array = mediaList.toArray(new Media[0]);


        String content = chatClient
                .prompt()
                .system(SYSTEM_PROMPT)
                .user(u -> u.text(message).media(array))
                .advisors(new MyLoggerAdvisor())
                .messages()
                .call().content();


//        context.get()
        String cleanJson = content
                .replaceAll("^\\s*```(?:json)?", "") // 去掉开头的 ``` 或 ```json
                .replaceAll("\\s*```\\s*$", "")      // 去掉结尾的 ```
                .replaceAll("\\\\", "")
                .trim();;



        TopicReportEnt topicReportEnt = JSON.parseObject(cleanJson, TopicReportEnt.class);
        return topicReportEnt;
    }

    public TopicJudgeReport multiJudgeCall(String SYSTEM_PROMPT, String message, List<Media> mediaList){

        Media[] array = mediaList.toArray(new Media[0]);

        TopicJudgeReport topicReport = chatClient
                .prompt()
                .system(SYSTEM_PROMPT)
                .user(u -> u.text(message).media(array))
                .advisors(new MyLoggerAdvisor())
                .call()
                .entity(TopicJudgeReport.class);
//        log.info("topicReport: {}", topicReport);
        return topicReport;
    }


    /*judge*/
    public record SubJudgeQuestions(String major_question_id,String question_id, List<String> answer_text,String question_type,String question_content,List<String> knowledge_points,String correct_answer,String is_correct,String feedback) {

    }
    public record TopicJudgeReport(String title, List<SubJudgeQuestions> answers, QuestionsMeta metadata) {

    }


    /*report*/
    public record SubQuestions(String major_question_id,String question_id, List<String> answer_text,String question_type,String question_content,List<String> knowledge_points) {

    }
    public record QuestionsMeta(String total_questions_detected) {

    }

    public record TopicReport(String title, List<SubQuestions> answers, QuestionsMeta metadata) {

    }



}
