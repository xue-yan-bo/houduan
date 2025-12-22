package com.jlm.homework.util;

import com.jlm.agent.AIServ.ZhiPuAIAgent;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class ZhipuAIImageStruDeal {

    @Autowired
    private ZhiPuAIAgent zhiPuAIServ;

    public ZhiPuAIAgent.TopicReport examQuestionAnalyst(){
        ArrayList<Media> medias = new ArrayList<>();
        ZhiPuAIAgent.TopicReport report = zhiPuAIServ.multiCall("","这是王五的卷子，结构化输出", medias);
        return report;
    }
}
