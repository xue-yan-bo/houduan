package com.jlm.homework.controller;

import com.jlm.homework.dto.StudentHomeworkDto;
import com.jlm.homework.dto.StudentsHomeworkRequest;
import com.jlm.homework.dto.StudentsHomeworkSimpleDTO;
import com.jlm.homework.entity.AuditLogoCoordinate;
import com.jlm.homework.entity.StudentsHomeworkNew;
import com.jlm.homework.exception.ParameterNewException;
import com.jlm.homework.service.IStudentsHomeworkNewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.alibaba.cloud.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        studentsHomework.setAuditStatus(1);
        studentsHomework=studentsHomeworkNewService.update(studentsHomework);
        return studentsHomework;
    }

    /**
     * 提交作业
     */
    @GetMapping("/appSubmit")
    public StudentsHomeworkNew appSubmit(@RequestParam(value = "studentsHomeworkId",  required = true)Long studentsHomeworkId ,
                                         @RequestParam(value = "submitFileUrl",  required = true)String submitFileUrl) throws Throwable {

        StudentsHomeworkNew studentsHomework=studentsHomeworkNewService.getById(studentsHomeworkId);
        studentsHomework.setSubmitFileUrl(submitFileUrl);
        studentsHomework.setSubmitStatus(1);
        studentsHomework.setSubmitTime(new Date());
        studentsHomework.setAuditStatus(1);
        studentsHomework=studentsHomeworkNewService.appSubmit(studentsHomework);
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

        studentsHomework.setAuditTime(new Date());
        
        // 根据老师审批坐标点判断学生作业错误逻辑
        judgeHomeworkErrorByCoordinate(studentsHomework);
        
        if(studentsHomework.getEmendStatus()!=null
                &&Integer.valueOf(2).compareTo(studentsHomework.getEmendStatus())==0){
            studentsHomework.setEmendStatus(3);
            studentsHomework.setAuditStatus(5);
        }else if(studentsHomework.getAuditStatus()==null
                ||Integer.valueOf(studentsHomework.getAuditStatus())<2){
            studentsHomework.setAuditStatus(2);
        }
        studentsHomework=studentsHomeworkNewService.audit(studentsHomework);
        return studentsHomework;
    }
    
    /**
     * 根据老师审批坐标点判断学生作业错误逻辑
     * @param studentsHomework 学生作业对象
     */
    private void judgeHomeworkErrorByCoordinate(StudentsHomeworkNew studentsHomework) {
        // 获取老师审批坐标
        List<AuditLogoCoordinate> auditCoordinates = studentsHomework.getAuditCoordinate();
        List<AuditLogoCoordinate> auditLogoCoordinates = studentsHomework.getAuditLogoCoordinate();
        
        // 存储错误原因和审批建议
        StringBuilder errorReasonBuilder = new StringBuilder();
        StringBuilder auditSuggestBuilder = new StringBuilder();
        
        // 判断是否有错误标记
        boolean hasError = false;
        
        // 处理老师审批坐标
        if (auditCoordinates != null && !auditCoordinates.isEmpty()) {
            hasError = true;
            errorReasonBuilder.append("发现").append(auditCoordinates.size()).append("处错误点\n");
            auditSuggestBuilder.append("请检查并修改标记的错误点\n");
            
            // 统计错误类型
            Map<String, Integer> errorTypeCount = new HashMap<>();
            for (AuditLogoCoordinate coord : auditCoordinates) {
                if (coord.getSymbol() != null) {
                    String symbolStr = coord.getSymbol().toString();
                    errorTypeCount.put(symbolStr, errorTypeCount.getOrDefault(symbolStr, 0) + 1);
                }
            }
            
            // 添加错误类型统计到错误原因
            for (Map.Entry<String, Integer> entry : errorTypeCount.entrySet()) {
                errorReasonBuilder.append("类型'").append(entry.getKey()).append("': "
                        + entry.getValue()).append("处\n");
            }
        }
        
        // 处理老师审批标识坐标
        if (auditLogoCoordinates != null && !auditLogoCoordinates.isEmpty()) {
            hasError = true;
            errorReasonBuilder.append("发现").append(auditLogoCoordinates.size()).append("处标识错误\n");
        }
        
        // 如果有错误，设置错误相关字段
        if (hasError&&studentsHomework.getEmendStatus()==null) {
            studentsHomework.setErrorReason(errorReasonBuilder.toString());
            studentsHomework.setTeacherAuditSuggest(auditSuggestBuilder.toString());
            // 设置需要订正
            studentsHomework.setEmendStatus(1);
            studentsHomework.setAuditStatus(3); // 3表示需要订正
        } else if(studentsHomework.getEmendStatus()==null
                &&studentsHomework.getAuditStatus()!=null
                &&Integer.valueOf(studentsHomework.getAuditStatus())<2){
            // 如果没有错误，设置为通过
            studentsHomework.setErrorReason("无错误");
            studentsHomework.setTeacherAuditSuggest("作业完成良好，继续保持！");
            studentsHomework.setAuditStatus(2); // 2表示审批通过
        }
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
     * 分页
     * @param studentsHomework
     * @return
     */
    @GetMapping("/emend/page")
    @Operation(summary = "分页查询")
    public Page<StudentsHomeworkSimpleDTO> getEmendPage(@RequestParam(defaultValue = "1")Integer pageNum,
                                                                   @RequestParam(defaultValue = "10") Integer pageSize,
                                                                   StudentsHomeworkNew studentsHomework) {
        Page<StudentsHomeworkSimpleDTO> studentsHomeworkList=studentsHomeworkNewService.getEmendPage(pageNum,pageSize,studentsHomework);
        return studentsHomeworkList;
    }
    /**
     * 分页
     * @param studentsHomework
     * @return
     */
    @GetMapping("/homeworkPage")
    @Operation(summary = "分页查询-带已提交、未提交数")
    public StudentHomeworkDto homeworkPage( @RequestParam(defaultValue = "1")Integer pageNum,
                                                              @RequestParam(defaultValue = "10") Integer pageSize,
                                                              StudentsHomeworkNew studentsHomework) {
        StudentHomeworkDto studentHomeworkDto=studentsHomeworkNewService.homeworkPage(pageNum,pageSize,studentsHomework);
        return studentHomeworkDto;
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
     * AI分析学生作业
     * @param studentsHomeworkId
     * @return
     */
    @GetMapping("/aIaudit")
    @Operation(summary = "AI分析学生作业")
    public String aIaudit(Long studentsHomeworkId){
        String auditAiImage=studentsHomeworkNewService.aIaudit(studentsHomeworkId);
        return auditAiImage;
    }

    /**
     * 订正提交作业
     */
    @GetMapping("/appEmendSubmit")
    @Operation(summary = "APP订正提交图片作业")
    public StudentsHomeworkNew appEmendSubmit(@RequestParam(value = "studentsHomeworkId",  required = true)Long studentsHomeworkId ,
                                         @RequestParam(value = "submitFileUrl2",  required = true)String submitFileUrl2) throws Throwable {

        StudentsHomeworkNew studentsHomework=studentsHomeworkNewService.getById(studentsHomeworkId);
        studentsHomework.setSubmitFileUrl2(submitFileUrl2);
        studentsHomework.setEmendStatus(2);
        studentsHomework.setAuditStatus(4);
        studentsHomework=studentsHomeworkNewService.appEmendSubmit(studentsHomework);
        return studentsHomework;
    }

    /**
     * AI分析学生作业
     * @param studentsHomeworkId
     * @return
     */
    @GetMapping("/aIauditEmend")
    @Operation(summary = "AI分析学生订正作业")
    public String aIauditEmend(Long studentsHomeworkId){
        String auditAiImage=studentsHomeworkNewService.aIauditEmend(studentsHomeworkId);
        return auditAiImage;
    }


}
