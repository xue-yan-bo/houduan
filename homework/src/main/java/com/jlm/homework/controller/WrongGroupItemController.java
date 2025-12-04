package com.jlm.homework.controller;

import com.jlm.homework.dto.ItemEditDto;
import com.jlm.homework.entity.WrongGroup;
import com.jlm.homework.entity.WrongGroupItem;
import com.jlm.homework.service.IWrongGroupItemService;
import com.jlm.homework.service.IWrongGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.alibaba.cloud.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "错题组卷题目", description = "错题组卷题目相关接口")
@RestController
@RequestMapping("/api/wrong-group-item")
public class WrongGroupItemController {
    @Autowired
    private IWrongGroupItemService wrongGroupItemService;
    @Autowired
    private IWrongGroupService wrongGroupService;

    /**
     * 题目列表
     *
     * @param wrongGroupId 错题题卷id
     * @return 题目列表
     */
    @Operation(summary = "题目列表查询")
    @GetMapping("/list")
    public List<WrongGroupItem> list(Long wrongGroupId){
        return wrongGroupItemService.listByGroupId(wrongGroupId);
    }
    /**
     * @param itemEditDto 题卷所有题目实体
     * @return 保存结果
     */
    @Operation(summary = "生成试题题目编辑")
    @PostMapping("/addOredit")
    @Transactional(rollbackFor = Exception.class)
    public void edit(@RequestBody ItemEditDto itemEditDto){
        if(itemEditDto==null){
            throw new RuntimeException("参数不能为空！");
        }
        WrongGroup wrongGroup = new WrongGroup();
        if(itemEditDto.getGroupId()==null){
            wrongGroup.setType(1);
            wrongGroup.setName(itemEditDto.getName());
            wrongGroup.setStudentId(itemEditDto.getStudentId());
            wrongGroup.setStudentName(itemEditDto.getStudentName());
            wrongGroup.setCreateTime(new Date());
            wrongGroup = wrongGroupService.addGroup(wrongGroup);
        }else{
            wrongGroup = wrongGroupService.getById(itemEditDto.getGroupId());
        }
        WrongGroup finalWrongGroup = wrongGroup;
        if(itemEditDto.getItemSaveDtoList()!=null&&itemEditDto.getItemSaveDtoList().size()>0){
            List<WrongGroupItem> itemEntities = itemEditDto.getItemSaveDtoList().stream().map(i -> {
                WrongGroupItem item = new WrongGroupItem();
                item.setWrongGroupId(finalWrongGroup.getId());
                item.setTitleType(i.getTitleType());
                if(StringUtils.isNotEmpty(i.getContent())) {
                    item.setContent(i.getContent());
                }
                if(StringUtils.isNotEmpty(i.getSolution())) {
                    item.setSolution(i.getSolution());
                }
                if(StringUtils.isNotEmpty(i.getParse())) {
                    item.setParse(i.getParse());
                }
                item.setSort(i.getSort());
                if (i.getId()!=null) {
                    item.setId(i.getId());
                }
                item.setUpdateTime(LocalDateTime.now());
                return item;
            }).collect(Collectors.toList());
            wrongGroupItemService.saveOrUpdateBatch(itemEntities) ;
            wrongGroupService.toWord(wrongGroup,itemEntities);
        }





    }



    /**
     * 删除题目
     *
     * @param groupId 题卷id
     * @param itemId  题目id
     * @return 删除结果
     */
    @Operation(summary = "题目删除")
    @GetMapping("/remove")
    private void remove(@RequestParam("groupId") Long groupId, @RequestParam("itemId") Long itemId) {
        WrongGroupItem item=wrongGroupItemService.findById(itemId);
        if(item==null){
            return;
        }
        // 题卷与题目统一判断
        if (!groupId.equals(item.getWrongGroupId())) {
            throw new RuntimeException("组卷数据已删除或不存在");
        }
        wrongGroupItemService.deleteById(itemId);
    }
}
