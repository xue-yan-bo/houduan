package com.jlm.homework.controller;

import com.jlm.homework.entity.ClassroomStudentWriteData;
import com.jlm.homework.service.IClassroomStudentWriteDataService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 课堂学生手写控制器
 * 提供课堂练习学生答题记录相关的REST接口
 */
@Tag(name = "课堂学生手写智能板", description = "课堂学生手写智能板的确认、下一页等")
@RestController
@RequestMapping("/api/classroom-student-write")
public class ClassroomStudentWriteDataController {
    @Autowired
    private IClassroomStudentWriteDataService classroomStudentWriteDataService;

    @PostMapping("/next")
    public void confirm(@RequestBody ClassroomStudentWriteData studentWriteData){
        classroomStudentWriteDataService.save(studentWriteData);
    }
}
