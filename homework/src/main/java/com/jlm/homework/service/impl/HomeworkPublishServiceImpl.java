package com.jlm.homework.service.impl;

import com.jlm.homework.entity.HomeworkPublish;
import com.jlm.homework.mapper.HomeworkPublishMapper;
import com.jlm.homework.service.IHomeworkPublishService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class HomeworkPublishServiceImpl implements IHomeworkPublishService {
    @Resource
    private HomeworkPublishMapper homeworkPublishMapper;
    @Override
    public String create(HomeworkPublish homeworkPublish) {
        if(homeworkPublish!=null
                &&homeworkPublish.getScheduledReleaseFlag()==0
                &&homeworkPublish.getPublishTime()==null){
            homeworkPublish.setPublishTime(new Date());
        }
        homeworkPublishMapper.save(homeworkPublish);
        return homeworkPublish.getId().toString();
    }

    @Override
    public HomeworkPublish getById(Long id) {
        return homeworkPublishMapper.getReferenceById(id);
    }

    @Override
    public HomeworkPublish update(HomeworkPublish homeworkPublish) {
        return homeworkPublishMapper.save(homeworkPublish);
    }

    @Override
    public Page<HomeworkPublish> selectList(Page<HomeworkPublish> page, HomeworkPublish homeworkPublish) {
        Pageable pageable = PageRequest.of(page.getNumber(), page.getSize(), page.getSort());
        homeworkPublish.setDeleteFlag(0);
        Example<HomeworkPublish>  example = Example.of(homeworkPublish);
        return homeworkPublishMapper.findAll(example,pageable);
    }

    @Override
    public void deleteById(Long id) {
        HomeworkPublish homeworkPublish=homeworkPublishMapper.getReferenceById(id);
        homeworkPublish.setDeleteFlag(1);
        homeworkPublishMapper.save(homeworkPublish);
    }
}
