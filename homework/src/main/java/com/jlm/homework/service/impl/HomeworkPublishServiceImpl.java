package com.jlm.homework.service.impl;

import com.jlm.homework.dto.HomeworkPublishRequest;
import com.jlm.homework.entity.ExerciseBookEntity;
import com.jlm.homework.entity.HomeworkPublish;
import com.jlm.homework.entity.StudentsHomeworkNew;
import com.jlm.homework.repository.HomeworkPublishRepository;
import com.jlm.homework.service.ExerciseBookServer;
import com.jlm.homework.service.IHomeworkPublishService;
import com.jlm.homework.service.IStudentsHomeworkNewService;
import jakarta.annotation.Resource;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class HomeworkPublishServiceImpl implements IHomeworkPublishService {
    @Resource
    private HomeworkPublishRepository homeworkPublishRepository;
    @Autowired
    private IStudentsHomeworkNewService studentsHomeworkNewService;
    @Autowired
    private ExerciseBookServer exerciseBookServer;
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
        String subject = null;
        if(homeworkPublish.getExerciseBookId() != null){
            ExerciseBookEntity exerciseBook = exerciseBookServer.findById(homeworkPublish.getExerciseBookId());
            subject =  exerciseBook.getSubject();
        }
        homeworkPublish.setSubject(subject);
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

        List<StudentsHomeworkNew> homeworkNewList=studentsHomeworkNewService.getByHomeworkPublishId(homeworkPublish.getId(),null);
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
        String subject = null;
        if(homeworkPublish.getExerciseBookId() != null){
            ExerciseBookEntity exerciseBook = exerciseBookServer.findById(homeworkPublish.getExerciseBookId());
            subject =  exerciseBook.getSubject();
        }
        homeworkPublish.setSubject(subject);
        return homeworkPublishRepository.save(homeworkPublish);
    }

    @Override
    public Page<HomeworkPublish> selectList(Integer pageNum,Integer pageSize, HomeworkPublishRequest homeworkPublishRequest) {
        if(pageNum==null||pageNum<=0||pageSize==null||pageSize<=0){
            pageNum=1;
            pageSize=10;
        }
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        Pageable pageable = PageRequest.of(pageNum-1, pageSize, sort);
        Specification<HomeworkPublish> specification = new Specification<HomeworkPublish>() {

            @Override
            public Predicate toPredicate(Root<HomeworkPublish> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                Predicate condition = null;
                if(StringUtils.isNotEmpty(homeworkPublishRequest.getHomeworkName())){
                    condition = criteriaBuilder.equal(root.get("homeworkName"),homeworkPublishRequest.getHomeworkName());
                }else {
                    condition= criteriaBuilder.conjunction();
                }
                Predicate cond1 = null;
                if(StringUtils.isNotEmpty(homeworkPublishRequest.getClassIds())) {
                    cond1 = criteriaBuilder.like(root.get("classIds"),"%"+homeworkPublishRequest.getClassIds()+"%");
                }else {
                    cond1  = criteriaBuilder.conjunction();
                }
                Predicate cond2 = null;
                Predicate cond3 = null;
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                Calendar calendar = Calendar.getInstance();
                try {

                    if(StringUtils.isNotEmpty(homeworkPublishRequest.getPublishTime())){

                        Date publishTime = sdf.parse(homeworkPublishRequest.getPublishTime());
                        calendar.setTime(publishTime);
                        calendar.add(Calendar.DAY_OF_MONTH,1);
                        Date publishTime1 = calendar.getTime();
                        cond2 =criteriaBuilder.between(root.get("publishTime"),publishTime,publishTime1);
                    }else {
                        cond2 = criteriaBuilder.conjunction();
                    }
                    if(StringUtils.isNotEmpty(homeworkPublishRequest.getDeadline())){

                        Date deadline = sdf.parse(homeworkPublishRequest.getDeadline());
                        calendar.setTime(deadline);
                        calendar.add(Calendar.DAY_OF_MONTH,1);
                        Date deadline1 = calendar.getTime();
                        cond3 =criteriaBuilder.between(root.get("deadline"),deadline,deadline1);
                    }else {
                        cond3 =criteriaBuilder.conjunction();
                    }
                }catch (ParseException e) {
                    throw new RuntimeException(e);
                }
                Predicate cond4 = null;
                if(homeworkPublishRequest.getTestSource()!=null){
                    cond4 =criteriaBuilder.equal(root.get("testSource"),homeworkPublishRequest.getTestSource());
                }else {
                    cond4 =criteriaBuilder.conjunction();
                }
                Predicate cond5 = null;
                if(homeworkPublishRequest.getAuditStatus()!=null){
                    cond5= criteriaBuilder.equal(root.get("auditStatus"),homeworkPublishRequest.getAuditStatus());
                }else {
                    cond5 =criteriaBuilder.conjunction();
                }
                Predicate cond6 = criteriaBuilder.equal(root.get("deleteFlag"),0);
                query.where(condition,cond1,cond2,cond3,cond4,cond5,cond6);
                return null;
            }
        };

        return homeworkPublishRepository.findAll(specification,pageable);
    }

    @Override
    public void deleteById(Long id) {
        HomeworkPublish homeworkPublish=homeworkPublishRepository.getReferenceById(id);
        homeworkPublish.setDeleteFlag(1);
        homeworkPublishRepository.save(homeworkPublish);
    }
}
