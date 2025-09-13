package com.jlm.homework.controller;

import com.jlm.homework.dto.*;
import com.jlm.homework.entity.StudentsHomeworkNew;
import com.jlm.homework.entity.StudentsHomeworkStatistics;
import com.jlm.homework.entity.WrongTitleStatistics;
import com.jlm.homework.service.IStudentsHomeworkNewService;
import com.jlm.homework.service.IStudentsHomeworkStatisticsService;
import com.jlm.homework.service.IWrongTitleStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Tag(name = "学生作业统计", description = "学生作业统计等")
@RestController
@RequestMapping("/api/students-homework-statistics")
public class StudentsHomeworkStatisticsController {
    @Autowired
    private IStudentsHomeworkStatisticsService studentsHomeworkStatisticsService;
    @Autowired
    private IWrongTitleStatisticsService wrongTitleStatisticsService;
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
        List<WrongTitleStatistics> wrongTitleStatisticses =  wrongTitleStatisticsService.getWrongTitleStatisticses(homeworkPublishId,classId);
        studentsHomeworkStatistics.setWrongTitleStatisticses(wrongTitleStatisticses);
        return studentsHomeworkStatistics;
    }

    /**
     * 平均正确率统计
     * @param
     * @return
     */
    @GetMapping("/average-accuracy-statistics")
    public AccuracyDto getAverageAccuracyStatistics(String subject, Long classId, String startDate, String endDate){
        AccuracyDto accuracyDto = studentsHomeworkNewService.getAverageAccuracyStatistics(subject,classId,startDate,endDate);
        return accuracyDto;
    }
    /**
     * 班级作业统计
     * @param
     * @return
     */
    @GetMapping("/class-homework-statistics")
    public List<StudentsHomeworkNew> getClassHomeworkStatistics(String subject,Long classId,String startDate,String endDate){
        List<StudentsHomeworkNew>  studentsHomeworkNewList =new ArrayList<>();
        studentsHomeworkNewList =studentsHomeworkNewService.getClassHomeworkStatistics(subject,classId,startDate,endDate);
        return studentsHomeworkNewList;
    }

    /**
     * 教材章节分析
     * @param
     * @return
     */
    @GetMapping("/homework-chapter-statistics")
    public List<StudentChapterAccuracy> homeworkChapterStatistics(String subject, Long classId, String chapter) {
        List<StudentChapterAccuracy> studentChapterAccuracyList = new ArrayList<>();
        studentChapterAccuracyList=studentsHomeworkNewService.studentChapterStatistics(subject,classId,chapter);
        return studentChapterAccuracyList;
    }
    /**
     * 章节知识点掌握情况
     * @param
     * @return
     */
    @GetMapping("/chapter-knowledge-analyse")
    public List<ChapterKnowledgeAccuracy> chapterKnowledgeAccuracy(String subject, Long classId, String startDate, String endDate){
        List<ChapterKnowledgeAccuracy>  chapterKnowledgeAccuracyList = studentsHomeworkNewService.chapterKnowledgeAccuracy(subject,classId,startDate,endDate);
        return  chapterKnowledgeAccuracyList;
    }
    /**
     * 学生家长可以查看单次作业的分析结果
     * @param
     * @return
     */
    @GetMapping("/student-wrongTitle-analyse")
    @Operation(summary = "学生家长可以查看单次作业的分析结果")
    public StudentsWrongTitleAnalyse studentsWrongTitleAnalyse(Long homeworkPublishId,Long classId,Long studentId){
        StudentsWrongTitleAnalyse  studentsWrongTitleAnalyse = new StudentsWrongTitleAnalyse();
        StudentsHomeworkStatistics studentsHomeworkStatistics= studentsHomeworkStatisticsService.getStudentsHomeworkStatistics(homeworkPublishId,classId);
        studentsWrongTitleAnalyse.setTitleTotal(studentsHomeworkStatistics.getTitleTotal());
        studentsWrongTitleAnalyse.setWrongTitleNum(studentsHomeworkStatistics.getWrongTitleNum());
        studentsWrongTitleAnalyse.setAverageCorrectness(studentsHomeworkStatistics.getAverageCorrectness());
        studentsWrongTitleAnalyse.setMaxCorrectness(studentsHomeworkStatistics.getMaxCorrectness());
        studentsWrongTitleAnalyse.setMinCorrectness(studentsHomeworkStatistics.getMinCorrectness());
        List<WrongTitleStatistics> wrongTitleBooks =  wrongTitleStatisticsService.getWrongTitleStatisticses(homeworkPublishId,classId);
        studentsWrongTitleAnalyse.setWrongTitleBooks(wrongTitleBooks);
        return studentsWrongTitleAnalyse;
    }

    /**
     * 校级智慧作业大数据
     * @param schoolId
     * @return
     */
    @GetMapping("/school-homework-data")
    @Operation(summary = "校级智慧作业大数据")
    public SchoolHomeworkData schoolHomeworkData(Long schoolId){
        SchoolHomeworkData schoolHomeworkData = new SchoolHomeworkData();
        schoolHomeworkData =studentsHomeworkNewService.getSchoolHomeworkData(schoolId);
        return schoolHomeworkData;
    }
    /**
     * 教育局作业大数据
     * @param educSearch
     * @return
     */
    @PostMapping("/educ-homework-data")
    @Operation(summary = "教育局作业大数据")
    public EducHomeworkData educHomeworkData(@RequestBody  EducSearch educSearch){
        EducHomeworkData educHomeworkData = new EducHomeworkData();
        Long educOrgId =educSearch.getEducOrgId();
        Long schoolId =educSearch.getSchoolId();
        String schoolType =educSearch.getSchoolType();
        educHomeworkData = studentsHomeworkNewService.getEducHomeworkData(educOrgId,schoolId,schoolType);
        return educHomeworkData;
    }

    /**
     * 作业统计
     * @param
     * @return
     */
    @GetMapping("/homework-statistics")
    @Operation(summary = "作业统计")
    public HomeworkStatisticsDto getHomeworkStatistics(String startDate,String endDate){
        HomeworkStatisticsDto statisticsDto= new HomeworkStatisticsDto();
        statisticsDto =studentsHomeworkNewService.getHomeworkStatistics(startDate,endDate);
        return statisticsDto;
    }

}
