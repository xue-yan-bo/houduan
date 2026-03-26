package com.jlm.homework.controller;

import com.jlm.homework.entity.QuestionBank;
import com.jlm.homework.entity.WrongGroup;
import com.jlm.homework.entity.WrongGroupItem;
import com.jlm.homework.repository.WrongGroupRepository;
import com.jlm.homework.service.IWrongGroupItemService;
import com.jlm.homework.service.IWrongGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "错题组卷", description = "错题组卷相关接口")
@RestController
@RequestMapping("/api/wrong-group")
public class WrongGroupController {
    @Autowired
    private IWrongGroupService wrongGroupService;
    @Autowired
    private IWrongGroupItemService wrongGroupItemService;
    @PostMapping("/addGroup")
    @Operation(summary = "增加错题组卷")
    public WrongGroup addGroup(@RequestBody WrongGroup wrongGroup) {
        return wrongGroupService.addGroup(wrongGroup);
    }
    @GetMapping("/{id}")
    public WrongGroup getById(@PathVariable Long id) {
        WrongGroup wrongGroup = wrongGroupService.getById(id);
        return wrongGroup;
    }

    /**
     * 更新
     */
    @PostMapping("/update")
    public WrongGroup update(@RequestBody WrongGroup wrongGroup) {
        wrongGroup=wrongGroupService.update(wrongGroup);
        return wrongGroup;


    }
    @Operation(summary = "生成文档")
    @GetMapping("/toWord")
    public void toWord(Long wrongGroupId) {
        WrongGroup wrongGroup = wrongGroupService.getById(wrongGroupId);
        List<WrongGroupItem> itemList=wrongGroupItemService.listByGroupId(wrongGroupId);
        wrongGroupService.toWord(wrongGroup,itemList);
    }

    /**
     * 分页查询
     * @param wrongGroup
     * @return
     */
    @GetMapping("/queryList")
    public Page<WrongGroup> selectPurchaseList(
            @RequestParam(defaultValue = "1")Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            WrongGroup wrongGroup) {

        Page<WrongGroup> list = wrongGroupService.selectList(pageNum,pageSize, wrongGroup);

        return list;
    }

    @DeleteMapping("/delete/{id}")
    public void deleteById(@PathVariable Long id) {
        wrongGroupService.deleteById(id);

    }
    
    /**
     * 更新状态
     */
    @PostMapping("/updateStatus")
    @Operation(summary = "更新错题组卷状态")
    public WrongGroup updateStatus(@RequestParam Long id, @RequestParam Integer status) {
        WrongGroup wrongGroup = wrongGroupService.getById(id);
        if (wrongGroup != null) {
            wrongGroup.setStatus(status);
            return wrongGroupService.update(wrongGroup);
        }
        return null;
    }
}
