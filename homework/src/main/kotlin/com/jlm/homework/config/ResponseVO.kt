package com.jlm.homework.config

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDateTime

/**
 * 统一返回结果封装类
 * 标准化所有API接口的返回格式
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ResponseVO<T>(
    /**
     * 响应状态码
     * 200: 成功
     * 400: 客户端错误
     * 500: 服务器错误
     */
    val code: Int,
    
    /**
     * 响应消息描述
     */
    val message: String,
    
    /**
     * 响应数据
     */
    val data: T? = null,
    
    /**
     * 是否成功
     */
    val success: Boolean = true,
    
    /**
     * 时间戳
     */
    val timestamp: Long = System.currentTimeMillis(),
    
    /**
     * 请求路径（用于错误追踪）
     */
    val path: String? = null
) {
    companion object {
        /**
         * 成功响应 - 带数据
         */
        fun <T> success(data: T, message: String = "操作成功"): ResponseVO<T> {
            return ResponseVO(
                code = 200,
                message = message,
                data = data,
                success = true
            )
        }
        
        /**
         * 成功响应 - 无数据
         */
        fun success(message: String = "操作成功"): ResponseVO<Any> {
            return ResponseVO(
                code = 200,
                message = message,
                data = null,
                success = true
            )
        }
        
        /**
         * 失败响应 - 通用错误
         */
        fun <T> error(code: Int = 500, message: String = "操作失败", path: String? = null): ResponseVO<T> {
            return ResponseVO(
                code = code,
                message = message,
                data = null,
                success = false,
                path = path
            )
        }
        
        /**
         * 失败响应 - 参数错误
         */
        fun <T> badRequest(message: String = "参数错误", path: String? = null): ResponseVO<T> {
            return ResponseVO(
                code = 400,
                message = message,
                data = null,
                success = false,
                path = path
            )
        }
        
        /**
         * 失败响应 - 资源未找到
         */
        fun <T> notFound(message: String = "资源未找到", path: String? = null): ResponseVO<T> {
            return ResponseVO(
                code = 404,
                message = message,
                data = null,
                success = false,
                path = path
            )
        }
        
        /**
         * 失败响应 - 服务器内部错误
         */
        fun <T> internalError(message: String = "服务器内部错误", path: String? = null): ResponseVO<T> {
            return ResponseVO(
                code = 500,
                message = message,
                data = null,
                success = false,
                path = path
            )
        }
    }
}

/**
 * 分页数据响应包装
 */
data class PageResponseVO<T>(
    /**
     * 数据列表
     */
    val records: List<T>,
    
    /**
     * 总记录数
     */
    val total: Long,
    
    /**
     * 当前页码
     */
    val current: Int,
    
    /**
     * 每页大小
     */
    val size: Int,
    
    /**
     * 总页数
     */
    val pages: Int
) 