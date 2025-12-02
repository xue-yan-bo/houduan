package com.jlm.homework.service;

import com.jlm.homework.feign.Result;
import com.jlm.homework.feign.SysFeignClient;
import com.jlm.homework.feign.SystemFeignClient;
import com.jlm.homework.feign.UserInfo;
import com.jlm.homework.util.UserContext;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Token服务类
 * 负责验证token并获取用户信息
 */
@Service
public class TokenService {
    
    private static final Logger logger = LoggerFactory.getLogger(TokenService.class);

    
    /**
     * 验证token并获取用户信息
     * @param token 传入的token
     * @return 是否验证成功
     */
    public boolean validateTokenAndGetUserInfo(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        
        try {
            // 这里可以根据实际情况实现token验证逻辑
            // 例如调用认证服务、解析JWT等

            // 示例实现：如果有sysFeignClient，则调用远程服务验证token
            
            // 简单的示例实现 - 从token中解析用户信息
            // 实际项目中应该使用更安全的方式，如JWT解析或调用认证服务
            if (token.startsWith("Bearer ")) {
                String actualToken = token.substring(7);
                // 这里可以解析JWT或其他格式的token
                // 示例：从token中提取用户信息（仅作演示）
                Map<String, String> userInfo = parseTokenForDemo(actualToken);
                if (userInfo != null) {
                    UserContext.setUserId(userInfo.get("userId"));
                    UserContext.setUsername(userInfo.get("username"));
                    UserContext.setToken(token);

                    return true;
                }
            }
            
            return true;
        } catch (Exception e) {
            logger.error("Token验证失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 演示用的token解析方法
     * 实际项目中应替换为真实的token解析逻辑
     */
    private Map<String, String> parseTokenForDemo(String token) {
        // 这里只是示例，实际项目中应使用JWT库解析token
        // 或者调用认证服务验证token
        
        // 简单示例：假设token格式为 "userId:username:timestamp"
        String[] parts = token.split(":");
        if (parts.length >= 2) {
            Map<String, String> userInfo = new java.util.HashMap<>();
            userInfo.put("userId", parts[0]);
            userInfo.put("username", parts[1]);
            return userInfo;
        }
        return null;
    }

    public Map<String, String> getUserInfoByCookie(String cookie) {
        String[] parts = cookie.split(";");
        if (parts.length >= 2) {
            Map<String, String> userInfo = new java.util.HashMap<>();
            for(int i = 1; i < parts.length; i++){
                String[] part = parts[i].split("=");
                String key = part[0].trim();
                String value = part[1].trim();
                userInfo.put(key, value);
            }
            if(userInfo.containsKey("userId")){
                UserContext.setUserId(userInfo.get("userId"));
            }
            if(userInfo.containsKey("username")){
                UserContext.setUsername(userInfo.get("username"));
            }
            if(userInfo.containsKey("Admin-Token")){
                UserContext.setToken(userInfo.get("Admin-Token"));
            }
            return userInfo;
        }
        return null;
    }
}