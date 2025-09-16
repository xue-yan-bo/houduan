package com.jlm.homework.service.impl;

import com.jlm.homework.entity.BookKnowledgePoint;
import com.jlm.homework.entity.ExerciseBookChapter;
import com.jlm.homework.entity.ExerciseBookQuestion;
import com.jlm.homework.repository.BookKnowledgePointRepository;
import com.jlm.homework.repository.ExerciseBookChapterRepository;
import com.jlm.homework.repository.ExerciseBookQuestionRepository;
import com.jlm.homework.service.IExerciseBookChapterService;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExerciseBookChapterServiceIpml implements IExerciseBookChapterService {
    @Resource
    private ExerciseBookChapterRepository exerciseBookChapterRepository;
    @Resource
    private BookKnowledgePointRepository bookKnowledgePointRepository;
    @Resource
    private ExerciseBookQuestionRepository exerciseBookQuestionRepository;
    @Override
    public void saveList(List<ExerciseBookChapter> list) {
        if (list.isEmpty()) {
            return;
        }
        ExerciseBookChapter chapter = new ExerciseBookChapter();
        chapter.setExerciseBookId(list.get(0).getExerciseBookId());
        exerciseBookChapterRepository.delete(chapter);
        for (ExerciseBookChapter exerciseBookChapter : list) {
            List<ExerciseBookQuestion> questionList = exerciseBookChapter.getQuestionList();
            List<BookKnowledgePoint> knowledgePointList = exerciseBookChapter.getKnowledgePointList();
            exerciseBookChapter = exerciseBookChapterRepository.save(exerciseBookChapter);
            BookKnowledgePoint point = new BookKnowledgePoint();
            point.setExerciseBookChapterId(exerciseBookChapter.getId());
            String knowledgePoints = "";
            bookKnowledgePointRepository.delete(point);
            for (BookKnowledgePoint bookKnowledgePoint : knowledgePointList) {
                bookKnowledgePoint.setExerciseBookChapterId(exerciseBookChapter.getId());
                bookKnowledgePoint.setExerciseBookId(exerciseBookChapter.getExerciseBookId());
                bookKnowledgePoint = bookKnowledgePointRepository.save(bookKnowledgePoint);
                knowledgePoints = bookKnowledgePoint.getKnowledgePoint() + ",";
            }
            ExerciseBookQuestion question = new ExerciseBookQuestion();
            question.setExerciseBookId(exerciseBookChapter.getExerciseBookId());
            question.setExerciseBookChapterId(exerciseBookChapter.getId());
            exerciseBookQuestionRepository.delete(question);
            if (questionList != null && !questionList.isEmpty()) {
                for (ExerciseBookQuestion exerciseBookQuestion : questionList) {
                    exerciseBookQuestion.setExerciseBookChapterId(exerciseBookChapter.getId());
                    exerciseBookQuestion.setExerciseBookId(exerciseBookChapter.getExerciseBookId());
                    exerciseBookQuestion.setKnowledgePoint(knowledgePoints);
                    exerciseBookQuestion.setKnowledgePoint(exerciseBookChapter.getKnowledgePointList().toString());
                    exerciseBookQuestionRepository.save(exerciseBookQuestion);
                }
            }
        }
    }
    @Override
    public List<ExerciseBookChapter> getByExerciseBookId(Long exerciseBookId) {
        ExerciseBookChapter chapter = new ExerciseBookChapter();
        chapter.setExerciseBookId(exerciseBookId);
        List<ExerciseBookChapter> chapterList = exerciseBookChapterRepository.findAll(Example.of(chapter));
        for (ExerciseBookChapter exerciseBookChapter : chapterList) {
            BookKnowledgePoint point = new BookKnowledgePoint();
            point.setExerciseBookChapterId(exerciseBookChapter.getId());
            point.setExerciseBookId(exerciseBookChapter.getExerciseBookId());
            List<BookKnowledgePoint> pointList = bookKnowledgePointRepository.findAll(Example.of(point));
            exerciseBookChapter.setKnowledgePointList(pointList);

            ExerciseBookQuestion question = new ExerciseBookQuestion();
            question.setExerciseBookChapterId(exerciseBookChapter.getId());
            question.setExerciseBookId(exerciseBookChapter.getExerciseBookId());
            List<ExerciseBookQuestion> questionList=exerciseBookQuestionRepository.findAll(Example.of(question));
            exerciseBookChapter.setQuestionList(questionList);

        }
        return chapterList;
    }
}
