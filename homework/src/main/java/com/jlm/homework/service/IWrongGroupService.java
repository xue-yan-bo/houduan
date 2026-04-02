package com.jlm.homework.service;

import com.jlm.homework.entity.WrongGroup;
import com.jlm.homework.entity.WrongGroupItem;
import org.springframework.data.domain.Page;

import java.util.List;

public interface IWrongGroupService {
    WrongGroup addGroup(WrongGroup wrongGroup);

    WrongGroup getById(Long id);

    WrongGroup update(WrongGroup wrongGroup);

    Page<WrongGroup> selectList(Integer pageNum, Integer pageSize, WrongGroup wrongGroup);

    void deleteById(Long id);

    void toWord(WrongGroup wrongGroup, List<WrongGroupItem> itemList);

    /**
     * 根据学生ID获取错题（截至指定时间）
     * @param studentId 学生ID
     * @param endTime 截至时间
     * @return 错题组列表
     */
    List<WrongGroup> getWrongGroupsByStudentId(Long studentId, java.util.Date endTime);

    /**
     * 保存错题改错结果
     * @param studentId 学生ID
     * @param wrongGroupId 错题组ID
     * @param wrongGroupItems
     * @return 保存结果
     */
    boolean saveErrorCorrectionRecords(Long studentId, Long wrongGroupId, List<WrongGroupItem> wrongGroupItems);
    

}
