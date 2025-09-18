package com.jlm.homework.service;

import com.jlm.homework.entity.CurrentUserInfo;
import com.jlm.homework.feign.School;

import java.util.List;

/**
 * 用户服务接口
 * 提供获取当前登录用户信息的功能
 */
public interface IUserService {
    
    /**
     * 获取当前登录用户ID
     * @return 用户ID，如果获取失败返回null
     */
    Long getCurrentUserId();
    
    /**
     * 获取默认用户ID
     * 当无法获取当前用户信息时使用
     */
    Long getDefaultUserId();
    
    /**
     * 安全获取当前用户ID
     * 提供更好的容错性，确保不会因为远程服务问题而阻塞业务
     */
    Long getCurrentUserIdSafely();
    
    /**
     * 获取当前登录用户信息
     * @return 用户信息，如果获取失败返回null
     */
    CurrentUserInfo getCurrentUserInfo();
    
    /**
     * 检查当前用户是否为教师
     * @return true如果是教师，false如果不是或获取失败
     */
    boolean isCurrentUserTeacher();
    
    /**
     * 获取当前登录用户所属学校信息
     * @return 学校信息，如果获取失败返回null
     */
    School getCurrentSchool();
    
    /**
     * 获取当前登录用户所属学校ID
     * @return 学校ID，如果获取失败返回默认学校ID
     */
    Long getCurrentSchoolId();
    
    /**
     * 安全获取当前学校ID
     * 提供更好的容错性，确保不会因为远程服务问题而阻塞业务
     */
    Long getCurrentSchoolIdSafely();
    
    /**
     * 获取默认学校ID
     * 当无法获取当前学校信息时使用
     */
    Long getDefaultSchoolId();
    

}