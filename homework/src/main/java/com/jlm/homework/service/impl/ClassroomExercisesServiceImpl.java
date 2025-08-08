package com.jlm.homework.service.impl;

import com.jlm.homework.entity.ClassroomExercises;
import com.jlm.homework.entity.ClassroomExercisesQuestion;
import com.jlm.homework.repository.ClassroomExercisesRepository;
import com.jlm.homework.service.IClassroomExercisesQuestionService;
import com.jlm.homework.service.IClassroomExercisesService;
import com.jlm.homework.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class ClassroomExercisesServiceImpl implements IClassroomExercisesService {
    @Resource
    private ClassroomExercisesRepository classroomExercisesRepository;
    @Autowired
    private IClassroomExercisesQuestionService classroomExercisesQuestionService;
    @Autowired
    private UserService userService;
    @Override
    public Long create(ClassroomExercises classroomExercises) {
        List<ClassroomExercisesQuestion> questionList=classroomExercises.getQuestionList();
        if(classroomExercises.getSchoolId()==null){
            classroomExercises.setSchoolId(userService.getCurrentSchoolIdSafely());
        }
        classroomExercises =classroomExercisesRepository.save(classroomExercises);
        classroomExercisesQuestionService.saveQuestionList(classroomExercises.getId(),questionList);
        return classroomExercises.getId();
    }

    @Override
    public ClassroomExercises getById(Long id) {
        ClassroomExercises  classroomExercises = classroomExercisesRepository.getById(id);
        if(classroomExercises != null){
            List<ClassroomExercisesQuestion> questionList=classroomExercisesQuestionService.selectQuestionList(classroomExercises.getId());
            classroomExercises.setQuestionList(questionList);
        }
        return classroomExercises;
    }

    @Override
    public ClassroomExercises update(ClassroomExercises classroomExercises) {
        List<ClassroomExercisesQuestion> questionList=classroomExercises.getQuestionList();
        classroomExercises = classroomExercisesRepository.save(classroomExercises);
        classroomExercisesQuestionService.saveQuestionList(classroomExercises.getId(),questionList);
        return classroomExercises;
    }

    @Override
    public Page<ClassroomExercises> selectList(Integer pageNum, Integer pageSize, ClassroomExercises classroomExercises) {
        pageNum = pageNum == null ? 0 : pageNum-1;
        pageSize = pageSize == null ? 10 : pageSize;
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        Pageable pageable = PageRequest.of(pageNum, pageSize, sort);
        Page<ClassroomExercises> page= classroomExercisesRepository.findAll(Example.of(classroomExercises),pageable);
        List<ClassroomExercises> exercisesList=page.getContent();
        for(ClassroomExercises item:exercisesList){
            List<ClassroomExercisesQuestion> questionList=classroomExercisesQuestionService.selectQuestionList(item.getId());
            item.setQuestionList(questionList);
        }

        return page;
    }

    @Override
    public void deleteById(Long id) {
        classroomExercisesRepository.deleteById(id);
    }

    @Override
    public ClassroomExercises publish(ClassroomExercises classroomExercises) {
        classroomExercises.setPublishStatus(1);
        classroomExercises.setPublishTime(new Date());
        List<ClassroomExercisesQuestion> questionList =classroomExercises.getQuestionList();
        classroomExercises = classroomExercisesRepository.save(classroomExercises);
        classroomExercisesQuestionService.saveQuestionList(classroomExercises.getId(),questionList);
        return classroomExercises;
    }
}
