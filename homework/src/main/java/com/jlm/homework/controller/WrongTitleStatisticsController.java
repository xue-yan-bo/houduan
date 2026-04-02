package com.jlm.homework.controller;

import com.jlm.homework.entity.WrongTitleBook;
import com.jlm.homework.entity.WrongTitleStatistics;
import com.jlm.homework.service.IWrongTitleStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@Tag(name = "错题统计", description = "错题统计相关接口")
@RestController
@RequestMapping("/api/wrong-title-book")
public class WrongTitleStatisticsController {
    @Autowired
    private IWrongTitleStatisticsService wrongTitleBookService;

    /**
     * 根据
     * @param homeworkPublishId
     * @return
     */
    @GetMapping("/createWrongTitleStatistics")
    public void createWrongTitleStatistics(Long homeworkPublishId,Long classId){
        wrongTitleBookService.createWrongTitleStatistics(homeworkPublishId,classId);
    }

    /**
     * 根据
     * @param wrongTitleBook
     * @return
     */
    @GetMapping("/page")
    public Page<WrongTitleStatistics> getWrongTitleBookList(@RequestParam(defaultValue = "1")Integer pageNum,
                                                        @RequestParam(defaultValue = "10") Integer pageSize,
                                                        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date startTime,
                                                        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")  Date endTime,
                                                            WrongTitleStatistics wrongTitleBook) {
        Page<WrongTitleStatistics> wrongTitleBookList=wrongTitleBookService.getPage(pageNum,pageSize,wrongTitleBook,startTime,endTime);
        return wrongTitleBookList;
    }
    @PostMapping("/updateClassWrongBook")
    @Operation(summary = "修改班级错题本")
    public void updateClassWrongBook(@RequestBody WrongTitleStatistics wrongTitleStatistics) {
        wrongTitleBookService.updateClassWrongBook(wrongTitleStatistics);
    }
    
    @DeleteMapping("/deleteClassWrong/{id}")
    @Operation(summary = "删除班级错题")
    public void deleteById(@PathVariable Long id) {
        wrongTitleBookService.deleteClassWrong(id);
    }

    @GetMapping("/classAiChart")
    @Operation(summary = "调用大模型解析错题")
    public void aiChart(Long id) {
        wrongTitleBookService.aiChart(id);
    }
}
