package com.jlm.homework.controller;

import com.jlm.homework.dto.StudentsHomeworkRequest;
import com.jlm.homework.entity.HomeworkPublish;
import com.jlm.homework.entity.StudentsHomeworkNew;
import com.jlm.homework.service.IStudentsHomeworkNewService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@Tag(name = "学生作业", description = "学生作业的查询、提交、审批等")
@RestController
@RequestMapping("/api/students-homework")
public class StudentsHomeworkNewController {
    @Autowired
    private IStudentsHomeworkNewService studentsHomeworkNewService;

    /**
     * 根据
     * @param homeworkPublishId
     * @return
     */
    @GetMapping("/publish/{homeworkPublishId}")
    public Page<StudentsHomeworkNew> getStudentsHomeworkList(@PathVariable Long homeworkPublishId,
                                                             @RequestParam(defaultValue = "1")Integer pageNum,
                                                             @RequestParam(defaultValue = "10") Integer pageSize,
                                                             StudentsHomeworkRequest studentsHomeworkRequest) {
        Page<StudentsHomeworkNew> studentsHomeworkList=studentsHomeworkNewService.getListByHomeworkPublishId(homeworkPublishId,pageNum,pageSize,studentsHomeworkRequest);
        return studentsHomeworkList;
    }
    @GetMapping("/{id}")
    public StudentsHomeworkNew getById(@PathVariable Long id) {
        StudentsHomeworkNew studentsHomeworkNew = studentsHomeworkNewService.getById(id);
        return studentsHomeworkNew;
    }
    /**
     * 提交作业
     */
    @PostMapping("/submit")
    public StudentsHomeworkNew submit(@RequestBody StudentsHomeworkNew studentsHomework) throws Throwable {
        if(StringUtils.isEmpty(studentsHomework.getSubmitFileUrl())){
            throw new RuntimeException("提交作业时作业文件路径不能为空！");
        }
        studentsHomework.setSubmitStatus(1);
        studentsHomework.setSubmitTime(new Date());
        studentsHomework.setAuditStatus("1");
        studentsHomework=studentsHomeworkNewService.update(studentsHomework);
        return studentsHomework;
    }

    /**
     * 审批作业
     */
    @PostMapping("/audit")
    public StudentsHomeworkNew audit(@RequestBody StudentsHomeworkNew studentsHomework) throws Throwable {
        studentsHomework.setAuditStatus("2");
        studentsHomework.setAuditTime(new Date());
        studentsHomework=studentsHomeworkNewService.update(studentsHomework);
        return studentsHomework;
    }

    /**
     * 根据
     * @param studentsHomework
     * @return
     */
    @GetMapping("/publish/page")
    public Page<StudentsHomeworkNew> getStudentsHomeworkPage( @RequestParam(defaultValue = "1")Integer pageNum,
                @RequestParam(defaultValue = "10") Integer pageSize,
                StudentsHomeworkNew studentsHomework) {
        Page<StudentsHomeworkNew> studentsHomeworkList=studentsHomeworkNewService.getStudentsHomeworkPage(pageNum,pageSize,studentsHomework);
        return studentsHomeworkList;
    }
}
