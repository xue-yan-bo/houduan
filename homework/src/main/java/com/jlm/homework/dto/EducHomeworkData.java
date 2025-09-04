package com.jlm.homework.dto;

import com.jlm.homework.entity.StudentsHomeworkNew;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class EducHomeworkData {
    //教育局下总体情况
    private Integer schoolNum;
    private Integer studentNum;
    private Integer homeworkNum;
    private Double homeworkAverageDuration;
    //学校数据
    private List<SchoolHomeworkNum> schoolHomeworkNumList;
    //年级作业时长统计
    private List<DurationStatistics>  durationStatisticsList;
    //各学校作业平均时长
    //private Map<String,Double> schoolAverageDuration;
    //平均作业时长整体走势
    private Map<String,Double> dayAverageDuration;
    //作业速报
    private List<StudentsHomeworkNew> studentsHomeworkNewList;


}
