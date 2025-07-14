package com.jlm.homework.service.impl;

import com.jlm.homework.entity.HomeworkPublish;
import com.jlm.homework.repository.HomeworkPublishRepository;
import com.jlm.homework.service.IHomeworkPublishService;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class HomeworkPublishServiceImpl implements IHomeworkPublishService {
    @Resource
    private HomeworkPublishRepository homeworkPublishRepository;
    @Override
    public String create(HomeworkPublish homeworkPublish) {
        if(homeworkPublish!=null
                &&homeworkPublish.getScheduledReleaseFlag()==0
                &&homeworkPublish.getPublishTime()==null){
            homeworkPublish.setPublishTime(new Date());
        }
        homeworkPublish.setDeleteFlag(0);
        homeworkPublishRepository.save(homeworkPublish);
        return homeworkPublish.getId().toString();
    }

    @Override
    public HomeworkPublish getById(Long id) {
        return homeworkPublishRepository.getById(id);
    }

    @Override
    public HomeworkPublish update(HomeworkPublish homeworkPublish) {
        homeworkPublish.setDeleteFlag(0);
        return homeworkPublishRepository.save(homeworkPublish);
    }

    @Override
    public Page<HomeworkPublish> selectList(Integer pageNum,Integer pageSize, HomeworkPublish homeworkPublish) {
        Pageable pageable = PageRequest.of(pageNum-1, pageSize);
        homeworkPublish.setDeleteFlag(0);
        Example<HomeworkPublish>  example = Example.of(homeworkPublish);
        return homeworkPublishRepository.findAll(example,pageable);
    }

    @Override
    public void deleteById(Long id) {
        HomeworkPublish homeworkPublish=homeworkPublishRepository.getReferenceById(id);
        homeworkPublish.setDeleteFlag(1);
        homeworkPublishRepository.save(homeworkPublish);
    }
}
