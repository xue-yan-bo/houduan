package com.jlm.homework.service.impl;

import com.jlm.homework.dto.ClassroomExercisesStudentStatistics;
import com.jlm.homework.entity.ClassroomExercisesStudentAnswer;
import com.jlm.homework.repository.ClassroomExercisesStudentAnswerRepository;
import com.jlm.homework.service.IClassroomExercisesStudentAnswerService;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ClassroomExercisesStudentAnswerServiceImpl implements IClassroomExercisesStudentAnswerService {
    @Resource
    private ClassroomExercisesStudentAnswerRepository classroomExercisesStudentAnswerRepository;

    @Override
    public ClassroomExercisesStudentAnswer save(ClassroomExercisesStudentAnswer studentAnswer) {
        return classroomExercisesStudentAnswerRepository.save(studentAnswer);
    }

    @Override
    public ClassroomExercisesStudentAnswer findById(Long id) {
        return classroomExercisesStudentAnswerRepository.findById(id).get();
    }

    @Override
    public List<ClassroomExercisesStudentAnswer> findByClassroomExercisesId(Long classroomExercisesId) {
        ClassroomExercisesStudentAnswer answer =new ClassroomExercisesStudentAnswer();
        answer.setClassroomExercisesId(classroomExercisesId);
        return classroomExercisesStudentAnswerRepository.findAll(Example.of(answer));
    }

    @Override
    public ClassroomExercisesStudentStatistics statisticsByClassroomExercisesId(Long classroomExercisesId) {
        ClassroomExercisesStudentStatistics statistics = new ClassroomExercisesStudentStatistics();
        List<ClassroomExercisesStudentAnswer> studentAnswerList = this.findByClassroomExercisesId(classroomExercisesId);
        Integer answerStudentNum = 0;
        Map<Integer,Integer> answerMap = new HashMap<>();
        for (ClassroomExercisesStudentAnswer studentAnswer : studentAnswerList) {
            if(answerMap.containsKey(studentAnswer.getTitleNumber())) {
                answerMap.put(studentAnswer.getTitleNumber(), answerMap.get(studentAnswer.getTitleNumber()) + 1);
            }else{
                answerMap.put(studentAnswer.getTitleNumber(), 1);
            }
        }
        return null;
    }
}
