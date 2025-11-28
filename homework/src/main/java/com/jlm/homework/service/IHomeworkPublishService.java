package com.jlm.homework.service;

import com.jlm.homework.dto.HomeworkPublishRequest;
import com.jlm.homework.entity.HomeworkPublish;
import org.springframework.data.domain.Page;

public interface IHomeworkPublishService {
    String create(HomeworkPublish homeworkPublish);

    HomeworkPublish getById(Long id);

    HomeworkPublish update(HomeworkPublish homeworkPublish);

    Page<HomeworkPublish> selectList(Integer pageNum,Integer pageSize, HomeworkPublishRequest homeworkPublishRequest);

    void deleteById(Long id);

    void withdraw(Long homeworkPublishId);

    void rePublish(Long homeworkPublishId);
}
