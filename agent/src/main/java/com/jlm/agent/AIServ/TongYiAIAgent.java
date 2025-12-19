//package com.jlm.agent.AIServ;
//
////import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
////import com.alibaba.dashscope.common.MultiModalMessage;
////import com.alibaba.dashscope.common.Role;
//import com.alibaba.fastjson2.JSON;
//import com.jlm.agent.advisor.MyLoggerAdvisor;
//import com.jlm.agent.domain.TopicReportEnt;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.ai.chat.client.ChatClient;
//import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
//import org.springframework.ai.chat.messages.SystemMessage;
//import org.springframework.ai.chat.messages.UserMessage;
//import org.springframework.ai.chat.model.ChatModel;
//import org.springframework.ai.content.Media;
//import org.springframework.stereotype.Component;
//import org.springframework.util.CollectionUtils;
//import org.springframework.util.MimeType;
//
//import java.net.URI;
//import java.net.URISyntaxException;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//
//import static org.springframework.ai.content.Media.Format.IMAGE_PNG;
//
//@Component
//@Slf4j
//public class TongYiAIAgent {
//
//    private final ChatClient chatClient;
//
//    /**
//     * 初始化 ChatClient
//     *
//     * @param dashscopeChatModel
//     */
//    public TongYiAIAgent(ChatModel dashscopeChatModel) {
//        chatClient = ChatClient.builder(dashscopeChatModel)
//                .defaultAdvisors()
//                .build();
//    }
//
//
//    public TopicReport multiCall(String SYSTEM_PROMPT, String message, String url)  {
//        Media media = null;
//        try {
//            media = new Media(IMAGE_PNG, new URI(url));
//        } catch (URISyntaxException e) {
//            throw new RuntimeException(e);
//        }
//
//
//        Media finalMedia = media;
//        TopicReport topicReport = chatClient
//                .prompt()
//                .system(SYSTEM_PROMPT)
//                .user(u -> u.text(message).media(finalMedia))
//                .call()
//                .entity(TopicReport.class);
////        log.info("topicReport: {}", topicReport);
//        return topicReport;
//    }
//
//    public TopicReport multiCall(String SYSTEM_PROMPT, String message, List<Media> mediaList){
//
//        Media[] array = mediaList.toArray(new Media[0]);
//
//        TopicReport topicReport = chatClient
//                .prompt()
//                .system(SYSTEM_PROMPT)
//                .user(u -> u.text(message).media(array))
//                .advisors(new MyLoggerAdvisor())
//                .messages()
//                .call()
//                .entity(TopicReport.class);
////        log.info("topicReport: {}", topicReport);
//        return topicReport;
//    }
//
////    public TopicReportEnt multiCallNoStruc(String SYSTEM_PROMPT, String message, List<Media> mediaList){
////
////        Media[] array = mediaList.toArray(new Media[0]);
////
////        String content = chatClient
////                .prompt()
////                .system(SYSTEM_PROMPT)
////                .user(u -> u.text(message).media(array))
////                .advisors(new MyLoggerAdvisor())
////                .messages()
////                .call().content();
////
////
//////        context.get()
////        String cleanJson = content
////                .replaceAll("^\\s*```(?:json)?", "") // 去掉开头的 ``` 或 ```json
////                .replaceAll("\\s*```\\s*$", "")      // 去掉结尾的 ```
////                .replaceAll("\\\\", "")
////                .trim();;
////
////
////
////        TopicReportEnt topicReportEnt = JSON.parseObject(cleanJson, TopicReportEnt.class);
////        return topicReportEnt;
////    }
//
//    public TopicReportEnt multiCallNoStruc(String SYSTEM_PROMPT, String message, List<Media> mediaList){
//
//
//        List<String> imageUrls = transMediaToStr(mediaList);
//
//        // 入参组装
//        ArrayList userParaList = new ArrayList();
//        userParaList.add(Collections.singletonMap("text", message));
//        if (!CollectionUtils.isEmpty(imageUrls)) {
//            for (String imageUrl : imageUrls) {
//                userParaList.add(Collections.singletonMap("image", imageUrl));
//            }
//        }
//
//        ArrayList sysParaList = new ArrayList();
//        sysParaList.add(Collections.singletonMap("text", SYSTEM_PROMPT));
//
//
//        // 调用接口
//        /*MultiModalConversation conv = new MultiModalConversation();
//
//        UserMessage.builder().role(Role.USER.getValue())
//                .content(userParaList).build();
//        MultiModalMessage sysMessage = MultiModalMessage.builder().role(Role.SYSTEM.getValue())
//                .content(sysParaList).build();*/
//
//
//        UserMessage build = UserMessage.builder().text(JSON.toJSONString(userParaList)).build();
//
//        String content = chatClient
//                .prompt()
//                .system(SYSTEM_PROMPT)
//                .messages(build)
////                .user(u -> u.text(message).media(array))
//                .advisors(new MyLoggerAdvisor())
//                .messages()
//                .call().content();
//
//
//    //        context.get()
//        String cleanJson = content
//                .replaceAll("^\\s*```(?:json)?", "") // 去掉开头的 ``` 或 ```json
//                .replaceAll("\\s*```\\s*$", "")      // 去掉结尾的 ```
//                .replaceAll("\\\\", "")
//                .trim();;
//
//
//
//        TopicReportEnt topicReportEnt = JSON.parseObject(cleanJson, TopicReportEnt.class);
//        return topicReportEnt;
//    }
//
//    public TopicJudgeReport multiJudgeCall(String SYSTEM_PROMPT, String message, List<Media> mediaList){
//
//        Media[] array = mediaList.toArray(new Media[0]);
//
//        TopicJudgeReport topicReport = chatClient
//                .prompt()
//                .system(SYSTEM_PROMPT)
//                .user(u -> u.text(message).media(array))
//                .advisors(new MyLoggerAdvisor())
//                .call()
//                .entity(TopicJudgeReport.class);
////        log.info("topicReport: {}", topicReport);
//        return topicReport;
//    }
//
//
//    /*judge*/
//    public record SubJudgeQuestions(String major_question_id,String question_id, List<String> answer_text,String question_type,String question_content,List<String> knowledge_points,String correct_answer,String is_correct,String feedback) {
//
//    }
//    public record TopicJudgeReport(String title, List<SubJudgeQuestions> answers, QuestionsMeta metadata) {
//
//    }
//
//
//    /*report*/
//    public record SubQuestions(String major_question_id,String question_id, List<String> answer_text,String question_type,String question_content,List<String> knowledge_points) {
//
//    }
//    public record QuestionsMeta(String total_questions_detected) {
//
//    }
//
//    public record TopicReport(String title, List<SubQuestions> answers, QuestionsMeta metadata) {
//
//    }
//
//
//    private List<String> transMediaToStr(List<Media> medias){
//
//        List<String> retList = new ArrayList<>();
//        for(Media media : medias){
//            MimeType mimeType = media.getMimeType();
//            String data = media.getData().toString();
//            retList.add("data:"+mimeType + ";base64," + data);
//        }
//        return retList;
//    }
//
//}
