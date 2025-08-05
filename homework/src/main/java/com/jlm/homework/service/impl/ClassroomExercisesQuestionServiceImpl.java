package com.jlm.homework.service.impl;

import com.jlm.homework.entity.ClassroomExercisesQuestion;
import com.jlm.homework.repository.ClassroomExercisesQuestionRepository;
import com.jlm.homework.service.IClassroomExercisesQuestionService;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClassroomExercisesQuestionServiceImpl implements IClassroomExercisesQuestionService {
    @Resource
    private ClassroomExercisesQuestionRepository classroomExercisesQuestionRepository;

    @Override
    public void saveQuestionList(Long classroomExercisesId, List<ClassroomExercisesQuestion> questionList) {
        if(questionList.isEmpty()){
            return;
        }
        questionList.stream().forEach(question -> question.setClassroomExercisesId(classroomExercisesId));
        classroomExercisesQuestionRepository.saveAll(questionList);
    }

    @Override
    public List<ClassroomExercisesQuestion>  selectQuestionList(Long classroomExercisesId) {
        ClassroomExercisesQuestion question = new ClassroomExercisesQuestion();
        question.setClassroomExercisesId(classroomExercisesId);
        List<ClassroomExercisesQuestion>  questionList= classroomExercisesQuestionRepository.findAll(Example.of(question));
        return questionList;
    }
}
