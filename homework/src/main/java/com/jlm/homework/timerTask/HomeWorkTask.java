package com.jlm.homework.timerTask;


import com.jlm.homework.entity.HomeworkPublish;
import com.jlm.homework.entity.StudentsHomeworkNew;
import com.jlm.homework.repository.HomeworkPublishRepository;
import com.jlm.homework.service.IStudentsHomeworkNewService;
import jakarta.annotation.Resource;
import jakarta.persistence.criteria.*;
import com.alibaba.cloud.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.scheduling.annotation.Scheduled;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Component
@EnableScheduling
@RestController
@RequestMapping("/homeWorkTask")
public class HomeWorkTask {
    @Resource
    private HomeworkPublishRepository homeworkPublishRepository;
    @Autowired
    private IStudentsHomeworkNewService studentsHomeworkNewService;
    @GetMapping("/deadlineStatus")
    @Scheduled(cron = "0 0 1 * * ?") // 每天凌晨1点执行
    public void deadlineStatus(){
        System.out.println("定时任务开始执行: " + LocalDateTime.now());
        Specification<HomeworkPublish> specification = new Specification<HomeworkPublish>() {

            @Override
            public Predicate toPredicate(Root<HomeworkPublish> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                Predicate condition = criteriaBuilder.lessThan( root.get("deadline").as(Date.class),new Date());

                Predicate cond1 = criteriaBuilder.notEqual(root.get("publishStatus"), 2);

                query.where(condition,cond1);
                return null;
            }
        };

        List<HomeworkPublish> publishList= homeworkPublishRepository.findAll(specification);
        if(publishList==null||publishList.size()==0){
            return;
        }
        for(HomeworkPublish publish:publishList){
            studentsHomeworkNewService.endStudentsHomework(publish);
            publish.setPublishStatus(2);
            homeworkPublishRepository.save(publish);
        }
        System.out.println("定时任务执行完成: " + LocalDateTime.now() + ", 更新了 " + publishList.size() + " 个作业的状态");
    }



}
