package com.jlm.homework.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.core.MethodParameter
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice

/**
 * 统一响应拦截器
 * 自动将Controller的返回值包装成统一的ResponseVO格式
 */
@RestControllerAdvice
class ResponseAdvice : ResponseBodyAdvice<Any> {

    private val objectMapper = ObjectMapper()

    /**
     * 判断是否需要处理响应
     * @param returnType 方法返回类型
     * @param converterType 消息转换器类型
     * @return true表示需要处理
     */
    override fun supports(
        returnType: MethodParameter,
        converterType: Class<out HttpMessageConverter<*>>
    ): Boolean {
        val declaringClass = returnType.declaringClass
        val methodName = returnType.method?.name
        
        // 排除已经是ResponseVO类型的返回值
        if (returnType.parameterType == ResponseVO::class.java) {
            return false
        }
        
        // 排除错误处理相关的方法
        if (declaringClass.name.contains("error", ignoreCase = true) ||
            declaringClass.name.contains("exception", ignoreCase = true)) {
            return false
        }
        
        // 排除Actuator健康检查端点
        if (declaringClass.packageName?.contains("actuator") == true) {
            return false
        }
        
        // 排除Swagger相关接口
        if (declaringClass.packageName?.contains("springfox") == true ||
            declaringClass.packageName?.contains("swagger") == true) {
            return false
        }
        
        return true
    }

    /**
     * 在响应体写入之前处理
     * @param body 响应体内容
     * @param returnType 方法返回类型
     * @param selectedContentType 选择的内容类型
     * @param selectedConverterType 选择的转换器类型
     * @param request 请求对象
     * @param response 响应对象
     * @return 处理后的响应体
     */
    override fun beforeBodyWrite(
        body: Any?,
        returnType: MethodParameter,
        selectedContentType: MediaType,
        selectedConverterType: Class<out HttpMessageConverter<*>>,
        request: ServerHttpRequest,
        response: ServerHttpResponse
    ): Any? {
        // 如果已经是ResponseVO类型，直接返回
        if (body is ResponseVO<*>) {
            return body
        }
        
        // 获取请求路径用于日志记录
        val requestPath = request.uri.path
        
        // 包装成功响应
        val responseVO = when (body) {
            null -> ResponseVO.success("操作成功")
            is String -> {
                // String类型需要特殊处理，转换为JSON字符串
                val successResponse = ResponseVO.success(body, "操作成功")
                return try {
                    objectMapper.writeValueAsString(successResponse)
                } catch (e: Exception) {
                    successResponse
                }
            }
            else -> ResponseVO.success(body, "操作成功")
        }
        
        return responseVO
    }
} 