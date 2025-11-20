package com.jlm.homework.service;

import com.jlm.homework.entity.ExerciseBookChapter;

import java.util.List;

public interface IExerciseBookChapterService {
    void saveList(List<ExerciseBookChapter> list);

    List<ExerciseBookChapter> getByExerciseBookId(Long exerciseBookId);

    List<String> findKnowledgePointByChapter(String chapter);
}
