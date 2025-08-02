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
import java.util.stream.Collectors;

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
        ExerciseBookChapter chapter=new ExerciseBookChapter();
        chapter.setExerciseBookId(list.get(0).getExerciseBookId());
        exerciseBookChapterRepository.delete(chapter);
        for (ExerciseBookChapter exerciseBookChapter : list) {

            exerciseBookChapter =exerciseBookChapterRepository.save(exerciseBookChapter);
            List<BookKnowledgePoint> knowledgePointList=exerciseBookChapter.getKnowledgePointList();
            BookKnowledgePoint point=new BookKnowledgePoint();
            point.setExerciseBookChapterId(exerciseBookChapter.getId());
            bookKnowledgePointRepository.delete(point);
            for (BookKnowledgePoint bookKnowledgePoint : knowledgePointList) {
                bookKnowledgePoint.setExerciseBookChapterId(exerciseBookChapter.getId());
                bookKnowledgePoint.setExerciseBookId(exerciseBookChapter.getExerciseBookId());
                bookKnowledgePoint =bookKnowledgePointRepository.save(bookKnowledgePoint);
                ExerciseBookQuestion question=new ExerciseBookQuestion();
                question.setExerciseBookId(exerciseBookChapter.getExerciseBookId());
                question.setExerciseBookChapterId(exerciseBookChapter.getId());
                exerciseBookQuestionRepository.delete(question);
                List<ExerciseBookQuestion> questionList =bookKnowledgePoint.getQuestionList();
                for (ExerciseBookQuestion exerciseBookQuestion : questionList) {
                    exerciseBookQuestion.setExerciseBookChapterId(exerciseBookChapter.getId());
                    exerciseBookQuestion.setExerciseBookId(exerciseBookChapter.getExerciseBookId());
                    exerciseBookQuestion.setBookKnowledgePointId(bookKnowledgePoint.getId());
                    exerciseBookQuestion.setKnowledgePoint(bookKnowledgePoint.getKnowledgePoint());
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
            for (BookKnowledgePoint bookKnowledgePoint : pointList) {
                ExerciseBookQuestion question = new ExerciseBookQuestion();
                question.setBookKnowledgePointId(bookKnowledgePoint.getId());
                question.setExerciseBookChapterId(exerciseBookChapter.getId());
                question.setExerciseBookId(exerciseBookChapter.getExerciseBookId());
                List<ExerciseBookQuestion> questionList=exerciseBookQuestionRepository.findAll(Example.of(question));
                bookKnowledgePoint.setQuestionList(questionList);
            }
            exerciseBookChapter.setKnowledgePointList(pointList);
        }
        return chapterList;
    }
}
