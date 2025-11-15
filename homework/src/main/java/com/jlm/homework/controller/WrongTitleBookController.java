package com.jlm.homework.controller;

import com.jlm.homework.entity.WrongTitleBook;
import com.jlm.homework.entity.WrongTitleStatistics;
import com.jlm.homework.service.IWrongTitleBookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "错题本", description = "错题本相关接口")
@RestController
@RequestMapping("/api/wrong-title-book")
public class WrongTitleBookController {
    @Autowired
    private IWrongTitleBookService wrongTitleBookService;

    @PostMapping("/addWrongBook")
    @Operation(summary = "加入错题本")
    public void addWrongBook(@RequestBody WrongTitleBook  wrongTitleBook) {
        wrongTitleBookService.addWrongBook(wrongTitleBook);
    }

    @GetMapping("/{studentId}")
    @Operation(summary = "学生错题本、我的错题本")
    public Page<WrongTitleBook> findByStudentId(@PathVariable("studentId") Long studentId,
                       @RequestParam(defaultValue = "1")Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize){
        return wrongTitleBookService.findByStudentId(studentId,pageNum,pageSize);
    }

    @GetMapping("/createWrongBook")
    public void createWrongBook(Long studentsHomeworkId){
        wrongTitleBookService.createWrongBook(studentsHomeworkId);
    }

    /**
     * 根据
     * @param wrongTitleBook
     * @return
     */
    @GetMapping("/pageList")
    public Page<WrongTitleBook> getWrongTitleBookList(@RequestParam(defaultValue = "1")Integer pageNum,
                                                            @RequestParam(defaultValue = "10") Integer pageSize,
                                                            WrongTitleBook wrongTitleBook) {
        Page<WrongTitleBook> wrongTitleBookList=wrongTitleBookService.getPage(pageNum,pageSize,wrongTitleBook);
        return wrongTitleBookList;
    }

    /**
     * 根据
     * @param wrongTitleId
     * @return
     */
    @GetMapping("/aiChart")
    public void aiChart(Long wrongTitleId){
        wrongTitleBookService.aiChart(wrongTitleId);
    }
}
