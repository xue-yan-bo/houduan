package com.jlm.agent.domain;

import java.util.List;

/**
 *  struc 题目
 */
public class SubQuestionsEnt {
    private String major_question_id;
    private String question_id;
    private List<String> answer_text;
    private String question_type;
    private String question_content;
    private List<String> knowledge_points;
    private List<String> correct_answer;
    private String is_correct;
    private String feedback;
    private String score; //得分
    private String question_score; //分值

    public String getQuestion_score() {
        return question_score;
    }

    public void setQuestion_score(String question_score) {
        this.question_score = question_score;
    }

    public String getMajor_question_id() {
        return major_question_id;
    }

    public void setMajor_question_id(String major_question_id) {
        this.major_question_id = major_question_id;
    }

    public String getQuestion_id() {
        return question_id;
    }

    public void setQuestion_id(String question_id) {
        this.question_id = question_id;
    }

    public List<String> getAnswer_text() {
        return answer_text;
    }

    public void setAnswer_text(List<String> answer_text) {
        this.answer_text = answer_text;
    }

    public String getQuestion_type() {
        return question_type;
    }

    public void setQuestion_type(String question_type) {
        this.question_type = question_type;
    }

    public String getQuestion_content() {
        return question_content;
    }

    public void setQuestion_content(String question_content) {
        this.question_content = question_content;
    }

    public List<String> getKnowledge_points() {
        return knowledge_points;
    }

    public void setKnowledge_points(List<String> knowledge_points) {
        this.knowledge_points = knowledge_points;
    }

    public List<String> getCorrect_answer() {
        return correct_answer;
    }

    public void setCorrect_answer(List<String> correct_answer) {
        this.correct_answer = correct_answer;
    }

    public String getIs_correct() {
        return is_correct;
    }

    public void setIs_correct(String is_correct) {
        this.is_correct = is_correct;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public String getScore() {
        return score;
    }

    public void setScore(String score) {
        this.score = score;
    }
}
