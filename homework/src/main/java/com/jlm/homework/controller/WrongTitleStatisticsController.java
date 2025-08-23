package com.jlm.homework.controller;

import com.jlm.homework.entity.WrongTitleStatistics;
import com.jlm.homework.service.IWrongTitleStatisticsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@Tag(name = "错题统计", description = "错题统计相关接口")
@RestController
@RequestMapping("/api/wrong-title-book")
public class WrongTitleStatisticsController {
    @Autowired
    private IWrongTitleStatisticsService wrongTitleBookService;

    /**
     * 根据
     * @param wrongTitleBook
     * @return
     */
    @GetMapping("/page")
    public Page<WrongTitleStatistics> getWrongTitleBookList(@RequestParam(defaultValue = "1")Integer pageNum,
                                                        @RequestParam(defaultValue = "10") Integer pageSize,
                                                            WrongTitleStatistics wrongTitleBook) {
        Page<WrongTitleStatistics> wrongTitleBookList=wrongTitleBookService.getPage(pageNum,pageSize,wrongTitleBook);
        return wrongTitleBookList;
    }
}
