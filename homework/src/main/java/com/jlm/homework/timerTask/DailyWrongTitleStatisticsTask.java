package com.jlm.homework.timerTask;

import com.jlm.homework.entity.CurrentUserInfo;
import com.jlm.homework.entity.HomeworkPublish;
import com.jlm.homework.repository.HomeworkPublishRepository;
import com.jlm.homework.service.IWrongTitleStatisticsService;
import jakarta.annotation.Resource;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 每日错题统计定时任务
 * 每天凌晨3点为昨日发布的作业生成错题统计
 */
@Component
public class DailyWrongTitleStatisticsTask implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(DailyWrongTitleStatisticsTask.class);

    @Resource
    private IWrongTitleStatisticsService wrongTitleStatisticsService;

    @Resource
    private HomeworkPublishRepository homeworkPublishRepository;

    // 方案一：使用Timer和TimerTask实现（与HomeworkPublishServiceImpl保持一致）
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // 防止重复初始化（在Spring父子容器情况下）
        if (event.getApplicationContext().getParent() == null) {
            logger.info("初始化每日错题统计定时任务...");
            startDailyWrongTitleStatisticsTask();
        }
    }

    /**
     * 启动每日错题统计定时任务
     */
    private void startDailyWrongTitleStatisticsTask() {
        // 计算首次执行时间：今天凌晨3点或明天凌晨3点
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 3);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        
        Date firstExecuteTime = calendar.getTime();
        
        // 如果当前时间已经过了今天凌晨3点，则首次执行时间设为明天凌晨3点
        if (firstExecuteTime.before(new Date())) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            firstExecuteTime = calendar.getTime();
        }
        
        // 定义定时任务
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                try {
                    logger.info("开始执行每日错题统计任务...");
                    generateDailyWrongTitleStatistics();
                    logger.info("每日错题统计任务执行完成");
                } catch (Exception e) {
                    logger.error("执行每日错题统计任务失败: {}", e.getMessage(), e);
                }
            }
        };
        
        // 定时任务间隔：24小时
        long period = 24 * 60 * 60 * 1000;
        
        // 启动定时任务
        Timer timer = new Timer("DailyWrongTitleStatisticsTask");
        timer.scheduleAtFixedRate(task, firstExecuteTime, period);
        
        logger.info("每日错题统计定时任务已启动，首次执行时间：{}", firstExecuteTime);
    }

    /**
     * 生成每日错题统计
     */
    private void generateDailyWrongTitleStatistics() {
        try {
            // 计算昨天的日期范围
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, -1);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            Date startOfYesterday = calendar.getTime();
            
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
            Date endOfYesterday = calendar.getTime();
            Specification<HomeworkPublish> specification = new Specification<HomeworkPublish>() {
                @Override
                public Predicate toPredicate(Root<HomeworkPublish> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                    Predicate condition = criteriaBuilder.between(root.get("publishTime"), startOfYesterday, endOfYesterday);
                    query.where(condition);
                    return null;
                }
            };

            // 查询昨天发布的作业
            List<HomeworkPublish> yesterdayPublishedHomeworks = homeworkPublishRepository.findAll(specification);
            if(yesterdayPublishedHomeworks==null||yesterdayPublishedHomeworks.size()==0){
                logger.info("昨天没有发布的作业！");
                return;
            }
            logger.info("找到昨天发布的作业数量: {}", yesterdayPublishedHomeworks.size());
            
            // 为每个作业生成错题统计
            for (HomeworkPublish homeworkPublish : yesterdayPublishedHomeworks) {
                try {
                    // 获取作业发布的班级ID列表
                    List<Long> classIds = homeworkPublish.getClassId();
                    if (classIds != null && !classIds.isEmpty()) {
                        for (Long classId : classIds) {
                            try {
                                logger.info("为作业[ID: {}, 名称: {}]的班级[ID: {}]生成错题统计",
                                        homeworkPublish.getId(), homeworkPublish.getHomeworkName(), classId);
                                wrongTitleStatisticsService.createWrongTitleStatistics(homeworkPublish.getId(), classId);
                                logger.info("作业[ID: {}]的班级[ID: {}]错题统计生成完成",
                                        homeworkPublish.getId(), classId);
                            } catch (Exception e) {
                                logger.error("为作业[ID: {}]的班级[ID: {}]生成错题统计失败: {}",
                                        homeworkPublish.getId(), classId, e.getMessage(), e);
                                // 继续处理下一个班级，不中断整体流程
                            }
                        }
                    } else {
                        logger.warn("作业[ID: {}, 名称: {}]没有关联的班级",
                                homeworkPublish.getId(), homeworkPublish.getHomeworkName());
                    }
                } catch (Exception e) {
                    logger.error("处理作业[ID: {}, 名称: {}]时发生错误: {}",
                            homeworkPublish.getId(), homeworkPublish.getHomeworkName(), e.getMessage(), e);
                    // 继续处理下一个作业，不中断整体流程
                }
            }
        } catch (Exception e) {
            logger.error("生成每日错题统计时发生严重错误: {}", e.getMessage(), e);
            throw e;
        }
    }


}
