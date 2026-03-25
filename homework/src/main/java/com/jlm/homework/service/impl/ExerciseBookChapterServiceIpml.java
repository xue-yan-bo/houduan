package com.jlm.homework.service.impl;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.jlm.homework.entity.BookKnowledgePoint;
import com.jlm.homework.entity.ExerciseBookChapter;
import com.jlm.homework.entity.ExerciseBookQuestion;
import com.jlm.homework.repository.BookKnowledgePointRepository;
import com.jlm.homework.repository.ExerciseBookChapterRepository;
import com.jlm.homework.repository.ExerciseBookQuestionRepository;
import com.jlm.homework.service.IExerciseBookChapterService;
import com.jlm.homework.util.WordToPdfUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
@Slf4j
@Service
public class ExerciseBookChapterServiceIpml implements IExerciseBookChapterService {
    @Resource
    private ExerciseBookChapterRepository exerciseBookChapterRepository;
    @Resource
    private BookKnowledgePointRepository bookKnowledgePointRepository;
    @Resource
    private ExerciseBookQuestionRepository exerciseBookQuestionRepository;
    @Autowired
    private WordToPdfUtil wordToPdfUtil;
    @Override
    public void saveList(List<ExerciseBookChapter> list) {
        if (list.isEmpty()) {
            return;
        }
        ExerciseBookChapter chapter = new ExerciseBookChapter();
        chapter.setExerciseBookId(list.get(0).getExerciseBookId());
        List<ExerciseBookChapter> chapters=exerciseBookChapterRepository.findAll(Example.of(chapter));
        if(chapters!=null&&chapters.size()>0){
            for(ExerciseBookChapter chapter1:chapters){
                exerciseBookChapterRepository.deleteById(chapter1.getId());
            }
        }

        for (ExerciseBookChapter exerciseBookChapter : list) {
            List<String> urls = new ArrayList<>();
            for(String url:exerciseBookChapter.getChapterDirectImages()) {
                if (StringUtils.isNotEmpty(url) &&
                        (url.contains(".docx") || url.contains(".doc"))) {
                    try {
                        String pdfurl = wordToPdfUtil.convertMinioWordToPdf(url);
                        urls.add(pdfurl);
                    } catch (Exception e) {
                        log.error("转换PDF错误：" + e.getMessage());
                        urls.add(url);
                    }
                } else {
                    urls.add(url);
                }
            }
            exerciseBookChapter.setChapterDirectImages(urls);
            List<ExerciseBookQuestion> questionList = exerciseBookChapter.getChapterDirectCropAreas();
            List<BookKnowledgePoint> knowledgePointList = exerciseBookChapter.getKnowledgePointList();
            exerciseBookChapter = exerciseBookChapterRepository.save(exerciseBookChapter);
            BookKnowledgePoint point = new BookKnowledgePoint();
            point.setExerciseBookChapterId(exerciseBookChapter.getId());
            String knowledgePoints = "";
            List<BookKnowledgePoint> points=bookKnowledgePointRepository.findAll(Example.of(point));
            if(points!=null&&points.size()>0){
                for(BookKnowledgePoint point1:points){
                    bookKnowledgePointRepository.deleteById(point1.getId());
                }
            }
            for (BookKnowledgePoint bookKnowledgePoint : knowledgePointList) {
                bookKnowledgePoint.setExerciseBookChapterId(exerciseBookChapter.getId());
                bookKnowledgePoint.setExerciseBookId(exerciseBookChapter.getExerciseBookId());
                bookKnowledgePoint = bookKnowledgePointRepository.save(bookKnowledgePoint);
                knowledgePoints = bookKnowledgePoint.getKnowledgePoint() + ",";
            }
            ExerciseBookQuestion question = new ExerciseBookQuestion();
            question.setExerciseBookId(exerciseBookChapter.getExerciseBookId());
            question.setExerciseBookChapterId(exerciseBookChapter.getId());
            List<ExerciseBookQuestion> questions=exerciseBookQuestionRepository.findAll(Example.of(question));
            if(questions!=null&&questions.size()>0){
                for(ExerciseBookQuestion question1:questions){
                    exerciseBookQuestionRepository.deleteById(question1.getId());
                }
            }
            if (questionList != null && !questionList.isEmpty()) {
                for (ExerciseBookQuestion exerciseBookQuestion : questionList) {
                    exerciseBookQuestion.setExerciseBookChapterId(exerciseBookChapter.getId());
                    exerciseBookQuestion.setExerciseBookId(exerciseBookChapter.getExerciseBookId());
                    exerciseBookQuestion.setKnowledgePoint(knowledgePoints);
                    exerciseBookQuestion.setPageNumber(list.indexOf(exerciseBookChapter)+1);
                    exerciseBookQuestion.setSourceImageUrl(exerciseBookQuestion.getSourceImageUrl());
                    exerciseBookQuestion.setCroppedUrl(exerciseBookQuestion.getCroppedUrl());
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
            exerciseBookChapter.setChapterDirectCropAreas(questionList);

        }
        return chapterList;
    }

    @Override
    public List<String> findKnowledgePointByChapter(String chapter) {
        List<ExerciseBookChapter> chapterList =exerciseBookChapterRepository.findChapterByName(chapter);
        List<String> stringList=new ArrayList<>();
        if(chapterList!=null&&chapterList.size()>0){
            for(ExerciseBookChapter exerciseBookChapter:chapterList){
                BookKnowledgePoint search = new BookKnowledgePoint();
                search.setExerciseBookChapterId(exerciseBookChapter.getId());
                List<BookKnowledgePoint> pointList=bookKnowledgePointRepository.findAll(Example.of(search));
                if(pointList!=null&&pointList.size()>0){
                    pointList.stream().forEach(point->{
                        stringList.add(point.getKnowledgePoint());
                    });
                }
            }
        }
        return stringList;
    }
}
