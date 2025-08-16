package com.jlm.homework.controller;

import com.jlm.homework.dto.StudentsHomeworkRequest;
import com.jlm.homework.entity.StudentsHomeworkNew;
import com.jlm.homework.entity.WrongTitleBook;
import com.jlm.homework.service.IWrongTitleBookService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@Tag(name = "错题本", description = "错题本相关接口")
@RestController
@RequestMapping("/api/wrong-title-book")
public class WrongTitleBookController {
    @Autowired
    private IWrongTitleBookService wrongTitleBookService;

    /**
     * 根据
     * @param wrongTitleBook
     * @return
     */
    @GetMapping("/page")
    public Page<WrongTitleBook> getWrongTitleBookList(@RequestParam(defaultValue = "1")Integer pageNum,
                                                        @RequestParam(defaultValue = "10") Integer pageSize,
                                                        WrongTitleBook wrongTitleBook) {
        Page<WrongTitleBook> wrongTitleBookList=wrongTitleBookService.getPage(pageNum,pageSize,wrongTitleBook);
        return wrongTitleBookList;
    }
}
