package com.jlm.homework.interceptor;

import com.jlm.homework.service.TokenService;
import com.jlm.homework.util.SecurityUtils;
import com.jlm.homework.util.UserContext;
import com.alibaba.cloud.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;

/**
 * Token拦截器
 * 用于拦截请求，提取token并验证用户身份
 */
@Component
public class TokenInterceptor implements HandlerInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(TokenInterceptor.class);
    
    @Autowired
    private TokenService tokenService;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 清除之前可能残留的用户上下文
        UserContext.clear();
        
        try {
            // 1. 从请求头中获取token
            String token = request.getHeader("Authorization");

            // 如果Authorization头不存在，尝试获取Admin-Token头
            if (token == null || token.trim().isEmpty()) {
                token = request.getHeader("Admin-Token");
            }
            //根据Cookie获取当前用户信息
            String cookie =  request.getHeader("Cookie");
            if(cookie != null && !cookie.trim().isEmpty()){
                Map<String, String>  info=tokenService.getUserInfoByCookie(cookie);
                if(info != null&& info.containsKey("username")){
                    String username = info.get("username");
                    UserContext.setUsername(username);
                    if (token == null || token.trim().isEmpty()) {
                        token = info.get("Admin-Token");
                    }
                }
            }

            // 如果token存在，验证并获取用户信息
            //if (token != null && !token.trim().isEmpty()) {
                //boolean isValid = tokenService.validateTokenAndGetUserInfo(token);
                //if (isValid) {
                  //  logger.debug("用户 {} 已通过token验证", UserContext.getUsername());

               // } else {
                    //logger.warn("无效的token: {}", token);
                    // 根据实际需求决定是否阻止请求继续
                    // 如果是需要认证的接口，可以返回401状态码
                    // response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    // return false;
               // }
           // }
            Long userId = SecurityUtils.getUserId();
            //System.out.println("当前用户id："+userId);
            if(StringUtils.isEmpty(UserContext.getUserId())){
                UserContext.setUserId(userId+"");
            }
            // 允许请求继续，即使没有token或token无效
            // 具体的权限控制可以在Controller层通过UserContext.isLoggedIn()判断
            return true;
        } catch (Exception e) {
            logger.error("Token拦截器异常: {}", e.getMessage(), e);
            // 出错时也允许请求继续，但不设置用户信息
            return true;
        }
    }
    
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        // 请求处理完成后不需要特别处理
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 请求完成后清除用户上下文，避免内存泄漏
        UserContext.clear();
    }
}