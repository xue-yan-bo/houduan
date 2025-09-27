package com.jlm.homework.controller;

import com.jlm.homework.entity.WrongTitleBook;
import com.jlm.homework.service.IWrongTitleBookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "错题本", description = "错题本相关接口")
@RestController
@RequestMapping("/api/wrong-title-book")
public class WrongTitleBookController {
    @Autowired
    private IWrongTitleBookService wrongTitleBookService;
    @GetMapping("/{studentId}")
    @Operation(summary = "学生错题本")
    public List<WrongTitleBook> findByStudentId(@PathVariable("studentId") Long studentId){
        return wrongTitleBookService.findByStudentId(studentId);
    }

    @GetMapping("/createWrongBook")
    public void createWrongBook(Long studentsHomeworkId){
        wrongTitleBookService.createWrongBook(studentsHomeworkId);
    }
}
