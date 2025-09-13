package com.jlm.homework.dto;

import com.jlm.homework.entity.HomeworkPublish;
import jakarta.persistence.criteria.CriteriaBuilder;
import lombok.Data;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class SchoolHomeworkData {
    //作业提交情况
    private Map<String,Double> homeworkSubmitSituation;
    //各年级作业提交情况
    private List<GradeHomeworkSubmit> gradeSubmitSituation;
    //练习册数据
    private ExerciseBookUseDate exerciseBookUse;
    //总体作业正确率
    private HomeworkRightRate totalRightRate;
    //作业率情况
    private Map<Integer,Integer> homeworkNumMap;
    //各年级作业正确率
    private List<HomeworkRightRate> gradesRightRateList;
    //教师批改情况
    private Map<Integer,Integer> teacherAuditMap;
    //今日作业提交情况
    private List<TodayHomeworkSubmit>  todayHomeworkSubmit;
    //今日作业批改情况
    private List<HomeworkPublish> todayHomeworkAuditList;

}


