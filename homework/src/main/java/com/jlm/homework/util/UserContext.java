package com.jlm.homework.util;

import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户上下文工具类
 * 用于在请求线程中存储和获取当前用户信息
 */
public class UserContext {
    
    private static final ThreadLocal<Map<String, Object>> USER_CONTEXT = ThreadLocal.withInitial(HashMap::new);
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_USER_INFO = "userInfo";
    
    /**
     * 设置当前用户ID
     */
    public static void setUserId(String userId) {
        USER_CONTEXT.get().put(KEY_USER_ID, userId);
    }
    
    /**
     * 获取当前用户ID
     */
    public static String getUserId() {
        return (String) USER_CONTEXT.get().get(KEY_USER_ID);
    }
    
    /**
     * 设置当前用户名
     */
    public static void setUsername(String username) {
        USER_CONTEXT.get().put(KEY_USERNAME, username);
    }
    
    /**
     * 获取当前用户名
     */
    public static String getUsername() {
        return (String) USER_CONTEXT.get().get(KEY_USERNAME);
    }
    
    /**
     * 设置当前token
     */
    public static void setToken(String token) {
        USER_CONTEXT.get().put(KEY_TOKEN, token);
    }
    
    /**
     * 获取当前token
     */
    public static String getToken() {
        return (String) USER_CONTEXT.get().get(KEY_TOKEN);
    }
    
    /**
     * 设置完整的用户信息对象
     */
    public static void setUserInfo(Object userInfo) {
        USER_CONTEXT.get().put(KEY_USER_INFO, userInfo);
    }
    
    /**
     * 获取完整的用户信息对象
     */
    @SuppressWarnings("unchecked")
    public static <T> T getUserInfo() {
        return (T) USER_CONTEXT.get().get(KEY_USER_INFO);
    }
    
    /**
     * 检查是否已登录
     */
    public static boolean isLoggedIn() {
        return StringUtils.hasText(getUserId()) && StringUtils.hasText(getToken());
    }
    
    /**
     * 清除当前线程的用户上下文
     * 必须在请求结束时调用，避免内存泄漏
     */
    public static void clear() {
        USER_CONTEXT.remove();
    }
    
    /**
     * 设置自定义属性
     */
    public static void setAttribute(String key, Object value) {
        USER_CONTEXT.get().put(key, value);
    }
    
    /**
     * 获取自定义属性
     */
    @SuppressWarnings("unchecked")
    public static <T> T getAttribute(String key) {
        return (T) USER_CONTEXT.get().get(key);
    }
}