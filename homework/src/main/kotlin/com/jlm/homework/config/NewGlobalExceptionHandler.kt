package com.jlm.homework.config

import com.jlm.homework.exception.*
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * 新的全局异常处理器
 * 使用ResponseVO统一返回格式处理所有异常
 */
@RestControllerAdvice
class NewGlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(NewGlobalExceptionHandler::class.java)

    /**
     * 处理业务异常
     * @param ex 业务异常
     * @return 统一的错误响应
     */
    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(ex: BusinessException): ResponseVO<Any> {
        logger.warn("业务异常: {}", ex.message)
        return ResponseVO.error(
            code = ex.code,
            message = ex.message ?: "业务处理失败"
        )
    }

    /**
     * 处理参数验证异常
     * @param ex 参数异常
     * @return 统一的错误响应
     */
    @ExceptionHandler(ParameterException::class)
    fun handleParameterException(ex: ParameterException): ResponseVO<Any> {
        logger.warn("参数验证异常: {}", ex.message)
        return ResponseVO.badRequest(
            message = ex.message ?: "参数错误"
        )
    }

    /**
     * 处理资源未找到异常
     * @param ex 资源未找到异常
     * @return 统一的错误响应
     */
    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleResourceNotFoundException(ex: ResourceNotFoundException): ResponseVO<Any> {
        logger.warn("资源未找到异常: {}", ex.message)
        return ResponseVO.notFound(
            message = ex.message ?: "资源未找到"
        )
    }

    /**
     * 处理权限不足异常
     * @param ex 权限异常
     * @return 统一的错误响应
     */
    @ExceptionHandler(PermissionDeniedException::class)
    fun handlePermissionDeniedException(ex: PermissionDeniedException): ResponseVO<Any> {
        logger.warn("权限不足异常: {}", ex.message)
        return ResponseVO.error(
            code = 403,
            message = ex.message ?: "权限不足"
        )
    }

    /**
     * 处理服务不可用异常
     * @param ex 服务不可用异常
     * @return 统一的错误响应
     */
    @ExceptionHandler(ServiceUnavailableException::class)
    fun handleServiceUnavailableException(ex: ServiceUnavailableException): ResponseVO<Any> {
        logger.warn("服务不可用异常: {}", ex.message)
        return ResponseVO.error(
            code = 503,
            message = ex.message ?: "服务暂时不可用"
        )
    }

    /**
     * 处理数据重复异常
     * @param ex 数据重复异常
     * @return 统一的错误响应
     */
    @ExceptionHandler(DuplicateDataException::class)
    fun handleDuplicateDataException(ex: DuplicateDataException): ResponseVO<Any> {
        logger.warn("数据重复异常: {}", ex.message)
        return ResponseVO.error(
            code = 409,
            message = ex.message ?: "数据已存在"
        )
    }

    /**
     * 处理数据状态异常
     * @param ex 数据状态异常
     * @return 统一的错误响应
     */
    @ExceptionHandler(DataStatusException::class)
    fun handleDataStatusException(ex: DataStatusException): ResponseVO<Any> {
        logger.warn("数据状态异常: {}", ex.message)
        return ResponseVO.error(
            code = 422,
            message = ex.message ?: "数据状态异常"
        )
    }

    /**
     * 处理运行时异常
     * @param ex 运行时异常
     * @return 统一的错误响应
     */
    @ExceptionHandler(RuntimeException::class)
    fun handleRuntimeException(ex: RuntimeException): ResponseVO<Any> {
        logger.error("运行时异常: {}", ex.message, ex)
        return ResponseVO.internalError(
            message = "服务内部错误，请稍后重试"
        )
    }

    /**
     * 处理通用异常
     * @param ex 异常
     * @return 统一的错误响应
     */
    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseVO<Any> {
        logger.error("未处理的异常: {}", ex.message, ex)
        return ResponseVO.internalError(
            message = "系统内部错误，请联系管理员"
        )
    }

    /**
     * 处理非法参数异常（系统级别）
     * @param ex 非法参数异常
     * @return 统一的错误响应
     */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException): ResponseVO<Any> {
        logger.warn("非法参数异常: {}", ex.message)
        return ResponseVO.badRequest(
            message = ex.message ?: "参数不合法"
        )
    }

    /**
     * 处理空指针异常
     * @param ex 空指针异常
     * @return 统一的错误响应
     */
    @ExceptionHandler(NullPointerException::class)
    fun handleNullPointerException(ex: NullPointerException): ResponseVO<Any> {
        logger.error("空指针异常: {}", ex.message, ex)
        return ResponseVO.internalError(
            message = "系统内部错误"
        )
    }
} 