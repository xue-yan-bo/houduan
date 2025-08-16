package com.jlm.homework.dto;

import lombok.Data;

import java.util.List;
@Data
public class HomeworkChapterStatistics {
    /**
     * 章节知识点掌握情况
     */
    private List<ChapterKnowledgeAccuracy> chapterKnowledgeList;
    /**
     * 教材章节分析
     */
    private List<StudentChapterAccuracy> studentChapterAccuracyList;
}
