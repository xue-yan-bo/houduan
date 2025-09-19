package com.jlm.homework.service.impl;

import com.jlm.homework.feign.*;
import com.jlm.homework.service.CurrentUserInfo;
import com.jlm.homework.service.ISysUserService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SysUserServiceImpl implements ISysUserService {
    Logger logger = LoggerFactory.getLogger(SysUserServiceImpl.class);
    @Autowired
    private Sys1FeignClient sys1FeignClient;
    @Autowired
    private System1FeignClient system1FeignClient;
    @Autowired
    private Teacher1FeignClient teacher1FeignClient;
    @Override
    public Long getCurrentUserIdSafely() {
        LoginUserInfo loginUserInfo = system1FeignClient.loginUserInfo();
        Long userId = null;
        if (loginUserInfo != null) {
            userId = loginUserInfo.getUserInfo().getUserId();
        }else {
            userId = 1000L;
        }
        return userId;
    }

    @Override
    public CurrentUserInfo getCurrentUserInfo() {
        LoginUserInfo loginUserInfo = system1FeignClient.loginUserInfo();
        CurrentUserInfo currentUserInfo = null;
        if (loginUserInfo != null&&loginUserInfo.getUserInfo()!=null) {
            UserInfo userInfo = loginUserInfo.getUserInfo();

            if(userInfo!=null){
                String teacherName = null;
                if(StringUtils.isNotEmpty(userInfo.getUserUuid())){
                    Teacher teacher= teacher1FeignClient.getTeacherByUserUuid(userInfo.getUserUuid());
                    teacherName = teacher.getName();
                }
                currentUserInfo = new CurrentUserInfo(userInfo.getUserId(),userInfo.getUserName(),userInfo.getNickName(),
                        userInfo.getUserUuid(),userInfo.getAdmin(),loginUserInfo.getRoles(),loginUserInfo.getCurrentRole(),
                        teacherName);
            }
        }

        return currentUserInfo;
    }

    @Override
    public Boolean isCurrentUserTeacher() {
        Boolean isCurrentUserTeacher = false;
        try {
            CurrentUserInfo userInfo = getCurrentUserInfo();

            if(StringUtils.isNotEmpty(userInfo.getTeacherName())){
                isCurrentUserTeacher= true;
            }
        } catch (Exception e) {
            logger.warn("检查用户是否为教师失败: {}", e.getMessage());
            isCurrentUserTeacher=false;
        }
        return isCurrentUserTeacher;
    }

    @Override
    public School getCurrentSchool() {
        try {
            School school = sys1FeignClient.currentSchool();
            if(school!=null){
                logger.info("获取当前学校信息: schoolId={}, schoolName={}", school.getSchoolId(), school.getSchoolName());
            }
            return school;
        }catch (Exception e){
            logger.error("获取当前学校信息失败: {}", e.getMessage());
            return null;
        }

    }

    @Override
    public Long getCurrentSchoolIdSafely() {
        Long schoolId = null;
        try {
            School school = getCurrentSchool();
            schoolId =school!=null?school.getSchoolId():1000l;
        } catch (Exception e) {
            logger.warn("获取当前学校ID失败，使用默认值: {}", e.getMessage());
            schoolId = 1000l;
        }
        return schoolId;
    }
}
