package com.jlm.homework.controller;

import com.jlm.homework.dto.StudentsHomeworkRequest;
import com.jlm.homework.entity.StudentsHomeworkNew;
import com.jlm.homework.exception.ParameterNewException;
import com.jlm.homework.service.IStudentsHomeworkNewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

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
    @Operation(summary = "根据老师发布作业查找学生作业")
    public Page<StudentsHomeworkNew> getStudentsHomeworkList(@PathVariable Long homeworkPublishId,
                                                             @RequestParam(defaultValue = "1")Integer pageNum,
                                                             @RequestParam(defaultValue = "10") Integer pageSize,
                                                             StudentsHomeworkRequest studentsHomeworkRequest) {
        Page<StudentsHomeworkNew> studentsHomeworkList=studentsHomeworkNewService.getListByHomeworkPublishId(homeworkPublishId,pageNum,pageSize,studentsHomeworkRequest);
        return studentsHomeworkList;
    }
    @GetMapping("/{id}")
    @Operation(summary = "查询详情")
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
    @Operation(summary = "审批作业")
    public StudentsHomeworkNew audit(@RequestBody StudentsHomeworkNew studentsHomework) throws Throwable {
        if(studentsHomework.getId()==null){
            throw new ParameterNewException("审批的学生作业ID不能为空!");
        }
        studentsHomework.setAuditStatus("2");
        studentsHomework.setAuditTime(new Date());
        if(studentsHomework.getEmendStatus()!=null){
            studentsHomework.setEmendStatus(3);
            studentsHomework.setAuditStatus("5");
        }
        studentsHomework=studentsHomeworkNewService.audit(studentsHomework);
        return studentsHomework;
    }

    /**
     * 分页
     * @param studentsHomework
     * @return
     */
    @GetMapping("/publish/page")
    @Operation(summary = "分页查询")
    public Page<StudentsHomeworkNew> getStudentsHomeworkPage( @RequestParam(defaultValue = "1")Integer pageNum,
                @RequestParam(defaultValue = "10") Integer pageSize,
                StudentsHomeworkNew studentsHomework) {
        Page<StudentsHomeworkNew> studentsHomeworkList=studentsHomeworkNewService.getStudentsHomeworkPage(pageNum,pageSize,studentsHomework);
        return studentsHomeworkList;
    }

    /**
     * 老师让学生订正作业接口
     * @param
     * @return
     */
    @GetMapping("/emend")
    @Operation(summary = "老师让学生订正作业接口")
    public void emend(Long studentsHomeworkId){
        studentsHomeworkNewService.emend(studentsHomeworkId);
        return;
    }

    /**
     * 订正分页
     * @param studentsHomework
     * @return
     */
    @GetMapping("/emend/page")
    @Operation(summary = "订正分页")
    public Page<StudentsHomeworkNew> geemendPage( @RequestParam(defaultValue = "1")Integer pageNum,
                                                              @RequestParam(defaultValue = "10") Integer pageSize,
                                                              StudentsHomeworkNew studentsHomework) {
        Page<StudentsHomeworkNew> studentsHomeworkList=studentsHomeworkNewService.geemendPage(pageNum,pageSize,studentsHomework);
        return studentsHomeworkList;
    }
}
