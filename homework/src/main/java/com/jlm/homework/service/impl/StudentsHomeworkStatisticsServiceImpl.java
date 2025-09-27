package com.jlm.homework.service.impl;

import com.jlm.homework.entity.StudentsHomeworkNew;
import com.jlm.homework.entity.StudentsHomeworkStatistics;
import com.jlm.homework.repository.StudentsHomeworkNewRepository;
import com.jlm.homework.repository.StudentsHomeworkStatisticsRepository;
import com.jlm.homework.service.IStudentsHomeworkStatisticsService;
import jakarta.annotation.Resource;
import jakarta.persistence.Column;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class StudentsHomeworkStatisticsServiceImpl implements IStudentsHomeworkStatisticsService {
    @Resource
    private StudentsHomeworkStatisticsRepository studentsHomeworkStatisticsRepository;
    @Resource
    private StudentsHomeworkNewRepository studentsHomeworkNewRepository;

    @Override
    public StudentsHomeworkStatistics getStudentsHomeworkStatistics(Long homeworkPublishId,Long classId) {
        StudentsHomeworkStatistics studentsHomeworkStatistics = new StudentsHomeworkStatistics();
        studentsHomeworkStatistics.setHomeworkPublishId(homeworkPublishId);
        studentsHomeworkStatistics.setClassId(classId);
        Example<StudentsHomeworkStatistics> example=Example.of(studentsHomeworkStatistics);
        Optional<StudentsHomeworkStatistics> optional= studentsHomeworkStatisticsRepository.findOne(example);
        if(optional.isEmpty()){
            return studentsHomeworkStatistics;
        }
        return optional.get();
    }

    @Override
    public Long insertStudentHomeworkStatistics(StudentsHomeworkStatistics studentsHomeworkStatistics) {
        studentsHomeworkStatistics=studentsHomeworkStatisticsRepository.save(studentsHomeworkStatistics);
        return studentsHomeworkStatistics.getId();
    }

    public void addStudentHomeworkStatistics(Long homeworkPublishId,Long classId) {
        StudentsHomeworkNew search=new StudentsHomeworkNew();
        search.setHomeworkPublishId(homeworkPublishId);
        search.setClassesId(classId);
        List<StudentsHomeworkNew> studentsHomeworkList=studentsHomeworkNewRepository.findAll(Example.of(search));
        StudentsHomeworkStatistics  studentsHomeworkStatistics = new StudentsHomeworkStatistics();
        studentsHomeworkStatistics.setHomeworkPublishId(homeworkPublishId);
        studentsHomeworkStatistics.setClassId(classId);
        studentsHomeworkStatistics.setStudentsSum(studentsHomeworkList.size());
        Integer submitStudentNum=0;

        Integer unsubmitStudentNum=0;
        Double fastestDuration=0.0;
        Double slowestDuration=0.0;
        Double totalDuration=0.0;
        for(StudentsHomeworkNew studentsHomework:studentsHomeworkList){
            if(1==studentsHomework.getSubmitStatus()){
                submitStudentNum++;
            }else{
                unsubmitStudentNum++;
            }
        }
    }
}
