package com.jlm.homework.service.impl;

import com.jlm.homework.entity.HomeworkPublish;
import com.jlm.homework.entity.StudentsHomeworkNew;
import com.jlm.homework.repository.HomeworkPublishRepository;
import com.jlm.homework.service.IHomeworkPublishService;
import com.jlm.homework.service.IStudentsHomeworkNewService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

@Service
public class HomeworkPublishServiceImpl implements IHomeworkPublishService {
    @Resource
    private HomeworkPublishRepository homeworkPublishRepository;
    @Autowired
    private IStudentsHomeworkNewService studentsHomeworkNewService;
    @Override
    public String create(HomeworkPublish homeworkPublish) {
        if(homeworkPublish!=null
                &&homeworkPublish.getScheduledReleaseFlag()==0
                &&homeworkPublish.getPublishTime()==null){
            homeworkPublish.setPublishTime(new Date());
        }
        homeworkPublish.setDeleteFlag(0);
        if(homeworkPublish.getClassId()!=null&&homeworkPublish.getClassId().size()>0){
            String classIds = homeworkPublish.getClassId().stream().map(Object::toString).collect(Collectors.joining(","));
            homeworkPublish.setClassIds(classIds);
        }
        if(homeworkPublish.getClassName()!=null&&homeworkPublish.getClassName().size()>0){
            homeworkPublish.setClassNames(String.join(",", homeworkPublish.getClassName()));
        }
        if(homeworkPublish.getTopicImages()!=null&&homeworkPublish.getTopicImages().size()>0){
            homeworkPublish.setTopicImagesStr(String.join(" ,", homeworkPublish.getTopicImages()));
        }
        if(homeworkPublish.getScheduledReleaseFlag()==0){
            homeworkPublish.setPublishStatus(1);
        }else {
            homeworkPublish.setPublishStatus(0);
        }
        homeworkPublishRepository.save(homeworkPublish);

        if(homeworkPublish.getScheduledReleaseFlag()==0){//如果不是定时发布，就是立刻发布，生成学生作业
            studentsHomeworkNewService.createStudentsHomeworkByHomeworkPublish(homeworkPublish);
        }else if(homeworkPublish.getScheduledReleaseFlag()==1
                &&homeworkPublish.getPublishTime()!=null){
            Timer timer = new Timer();
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    studentsHomeworkNewService.createStudentsHomeworkByHomeworkPublish(homeworkPublish);
                    homeworkPublish.setPublishStatus(1);
                    homeworkPublishRepository.save(homeworkPublish);
                }
            };
            timer.schedule(task,homeworkPublish.getPublishTime());
        }
        return homeworkPublish.getId().toString();
    }

    @Override
    public HomeworkPublish getById(Long id) {
        return homeworkPublishRepository.getById(id);
    }

    @Override
    public HomeworkPublish update(HomeworkPublish homeworkPublish) {
        homeworkPublish.setDeleteFlag(0);
        if(homeworkPublish.getClassId()!=null&&homeworkPublish.getClassId().size()>0){
            String classIds = homeworkPublish.getClassId().stream().map(Object::toString).collect(Collectors.joining(","));
            homeworkPublish.setClassIds(classIds);
        }
        if(homeworkPublish.getClassName()!=null&&homeworkPublish.getClassName().size()>0){
            homeworkPublish.setClassNames(String.join(",", homeworkPublish.getClassName()));
        }
        if(homeworkPublish.getTopicImages()!=null&&homeworkPublish.getTopicImages().size()>0){
            homeworkPublish.setTopicImagesStr(String.join(" ,", homeworkPublish.getTopicImages()));
        }

        List<StudentsHomeworkNew> homeworkNewList=studentsHomeworkNewService.getByHomeworkPublishId(homeworkPublish.getId());
        if(homeworkNewList.isEmpty()){
            if(homeworkPublish.getScheduledReleaseFlag()==0){//如果不是定时发布，就是立刻发布，生成学生作业
                studentsHomeworkNewService.createStudentsHomeworkByHomeworkPublish(homeworkPublish);
            }else if(homeworkPublish.getScheduledReleaseFlag()==1
                    &&homeworkPublish.getPublishTime()!=null){
                Timer timer = new Timer();
                TimerTask task = new TimerTask() {
                    @Override
                    public void run() {
                        studentsHomeworkNewService.createStudentsHomeworkByHomeworkPublish(homeworkPublish);
                        homeworkPublish.setPublishStatus(1);
                        homeworkPublishRepository.save(homeworkPublish);
                    }
                };
                timer.schedule(task,homeworkPublish.getPublishTime());
            }
        }
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
