package com.jlm.agent.domain;

import com.jlm.agent.AIServ.ZhiPuAIAgent;

import java.util.List;

public class TopicReportEnt {

    private String title;
    private List<SubQuestionsEnt> answers;
    private QuestionsMetaEnt metadata;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<SubQuestionsEnt> getAnswers() {
        return answers;
    }

    public void setAnswers(List<SubQuestionsEnt> answers) {
        this.answers = answers;
    }

    public QuestionsMetaEnt getMetadata() {
        return metadata;
    }

    public void setMetadata(QuestionsMetaEnt metadata) {
        this.metadata = metadata;
    }
}
