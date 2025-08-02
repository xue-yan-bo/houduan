package com.jlm.homework.service.impl;

import com.jlm.homework.repository.ClassroomExercisesQuestionRepository;
import com.jlm.homework.service.IClassroomExercisesQuestionService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class ClassroomExercisesQuestionServiceImpl implements IClassroomExercisesQuestionService {
    @Resource
    private ClassroomExercisesQuestionRepository classroomExercisesQuestionRepository;
}
