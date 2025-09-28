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

import java.math.BigDecimal;
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
        Double totalAccuracy=0.0;
        for(StudentsHomeworkNew studentsHomework:studentsHomeworkList){
            if(1==studentsHomework.getSubmitStatus()){
                submitStudentNum++;
            }else{
                unsubmitStudentNum++;
            }

            if(studentsHomework.getStartTime()!=null&&studentsHomework.getSubmitTime()!=null ){
                double duration = 0.0;
                duration =  (studentsHomework.getSubmitTime().getTime() - studentsHomework.getStartTime().getTime())/1000/60;
                totalDuration  += duration;
                if(duration!=0.0&&duration<fastestDuration){
                    fastestDuration = duration;
                }
                if(duration!=0.0&&duration>slowestDuration){
                    slowestDuration = duration;
                }
            }
            if(studentsHomework.getAccuracy()!=null){
                totalAccuracy +=studentsHomework.getAccuracy();
            }
        }
        studentsHomeworkStatistics.setSubmitStudentNum(submitStudentNum);
        studentsHomeworkStatistics.setUnsubmitStudentNum(unsubmitStudentNum);
        Double submitRate = 0.0d;
        if(studentsHomeworkList.size()>0){
            submitRate = BigDecimal.valueOf(submitStudentNum).divide(BigDecimal.valueOf(studentsHomeworkList.size()),4,BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue();
        }
        Double unsubmitRate = 0.0d;
        if(studentsHomeworkList.size()>0){
            unsubmitRate = BigDecimal.valueOf(unsubmitStudentNum).divide(BigDecimal.valueOf(studentsHomeworkList.size()),4,BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue();
        }
        studentsHomeworkStatistics.setSubmitRate(submitRate);
        studentsHomeworkStatistics.setUnsubmitRate(unsubmitRate);
        studentsHomeworkStatistics.setFastestDuration(fastestDuration);
        studentsHomeworkStatistics.setSlowestDuration(slowestDuration);
        studentsHomeworkStatistics.setAverageDuration(totalDuration/submitStudentNum);
        studentsHomeworkStatistics.setAverageAccuracy(totalAccuracy/studentsHomeworkList.size());
        studentsHomeworkStatistics.setAverageCorrectness(totalAccuracy/studentsHomeworkList.size());
        studentsHomeworkStatistics.setCompareLast(0.0);
        studentsHomeworkStatisticsRepository.save(studentsHomeworkStatistics);
    }
}
