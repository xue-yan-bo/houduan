package com.jlm.homework.controller;

import com.jlm.homework.dto.AverageAccuracyDto;
import com.jlm.homework.entity.StudentsHomeworkNew;
import com.jlm.homework.entity.StudentsHomeworkStatistics;
import com.jlm.homework.entity.WrongTitleBook;
import com.jlm.homework.service.IStudentsHomeworkNewService;
import com.jlm.homework.service.IStudentsHomeworkStatisticsService;
import com.jlm.homework.service.IWrongTitleBookService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@Tag(name = "学生作业统计", description = "学生作业统计等")
@RestController
@RequestMapping("/api/students-homework-statistics")
public class StudentsHomeworkStatisticsController {
    @Autowired
    private IStudentsHomeworkStatisticsService studentsHomeworkStatisticsService;
    @Autowired
    private IWrongTitleBookService wrongTitleBookService;
    @Autowired
    private IStudentsHomeworkNewService studentsHomeworkNewService;


    /**
     * 批改作业统计
     * @param homeworkPublishId
     * @return
     */
    @GetMapping("/correct-homework-statistics/{homeworkPublishId}")
    public StudentsHomeworkStatistics getStudentsHomeworkStatistics(@PathVariable Long homeworkPublishId,Long classId){
        StudentsHomeworkStatistics studentsHomeworkStatistics= studentsHomeworkStatisticsService.getStudentsHomeworkStatistics(homeworkPublishId,classId);
        List<WrongTitleBook> wrongTitleBooks =  wrongTitleBookService.getWrongTitleBooks(homeworkPublishId,classId);
        studentsHomeworkStatistics.setWrongTitleBooks(wrongTitleBooks);
        return studentsHomeworkStatistics;
    }

    /**
     * 平均正确率统计
     * @param
     * @return
     */
    @GetMapping("/average-accuracy-statistics")
    public List<AverageAccuracyDto> getAverageAccuracyStatistics(String subject,Long classId,String startDate,String endDate){
        List<AverageAccuracyDto>  averageAccuracyDtos =new ArrayList<>();
        averageAccuracyDtos = studentsHomeworkNewService.getAverageAccuracyStatistics(subject,classId,startDate,endDate);
        return averageAccuracyDtos;
    }
    /**
     * 班级作业统计
     * @param
     * @return
     */
    @GetMapping("/class-homework-statistics")
    public List<StudentsHomeworkNew> getClassHomeworkStatistics(String subject,Long classId,String startDate){
        List<StudentsHomeworkNew>  studentsHomeworkNewList =new ArrayList<>();
        studentsHomeworkNewList =studentsHomeworkNewService.getClassHomeworkStatistics(subject,classId,startDate);
        return studentsHomeworkNewList;
    }
}
