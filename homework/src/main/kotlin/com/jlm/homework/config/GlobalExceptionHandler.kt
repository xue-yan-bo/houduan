package com.jlm.homework.config

import feign.FeignException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import java.time.LocalDateTime

/**
 * 全局异常处理器
 * 统一处理应用中的异常，提供标准化的错误响应
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    /**
     * 处理Feign异常
     */
    @ExceptionHandler(FeignException::class)
    fun handleFeignException(
        ex: FeignException,
        request: WebRequest
    ): ResponseEntity<Map<String, Any>> {
        logger.error("Feign调用异常: {} - {}", ex.status(), ex.contentUTF8(), ex)

        val errorResponse = mapOf(
            "success" to false,
            "error" to mapOf(
                "code" to "FEIGN_CALL_ERROR",
                "message" to "远程服务调用失败",
                "httpStatus" to ex.status(),
                "details" to ex.contentUTF8(),
                "timestamp" to LocalDateTime.now().toString(),
                "path" to request.getDescription(false)
            )
        )

        val httpStatus = when (ex.status()) {
            400 -> HttpStatus.BAD_REQUEST
            401 -> HttpStatus.UNAUTHORIZED
            403 -> HttpStatus.FORBIDDEN
            404 -> HttpStatus.NOT_FOUND
            500 -> HttpStatus.INTERNAL_SERVER_ERROR
            503 -> HttpStatus.SERVICE_UNAVAILABLE
            else -> HttpStatus.SERVICE_UNAVAILABLE
        }

        return ResponseEntity.status(httpStatus).body(errorResponse)
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(
        ex: IllegalArgumentException,
        request: WebRequest
    ): ResponseEntity<Map<String, Any>> {
        logger.warn("非法参数异常: {}", ex.message)
        
        val errorResponse = mapOf(
            "success" to false,
            "error" to mapOf(
                "code" to "INVALID_PARAMETER",
                "message" to "参数错误",
                "details" to (ex.message ?: "参数不合法"),
                "timestamp" to LocalDateTime.now().toString(),
                "path" to request.getDescription(false)
            )
        )
        
        return ResponseEntity.badRequest().body(errorResponse)
    }

    /**
     * 处理运行时异常
     */
    @ExceptionHandler(RuntimeException::class)
    fun handleRuntimeException(
        ex: RuntimeException,
        request: WebRequest
    ): ResponseEntity<Map<String, Any>> {
        logger.error("运行时异常: {}", ex.message, ex)
        
        val errorResponse = mapOf(
            "success" to false,
            "error" to mapOf(
                "code" to "RUNTIME_ERROR",
                "message" to "服务内部错误",
                "details" to (ex.message ?: "未知运行时错误"),
                "timestamp" to LocalDateTime.now().toString(),
                "path" to request.getDescription(false)
            )
        )
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }

    /**
     * 处理JSON解析异常（如参数格式错误、类型不匹配等）
     */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(
        ex: HttpMessageNotReadableException,
        request: WebRequest
    ): ResponseEntity<Map<String, Any>> {
        logger.warn("参数解析异常: {}", ex.message, ex)

        val errorResponse = mapOf(
            "success" to false,
            "error" to mapOf(
                "code" to "JSON_PARSE_ERROR",
                "message" to "请求参数格式错误，请检查字段类型和格式",
                "details" to (ex.message ?: "JSON解析失败"),
                "timestamp" to LocalDateTime.now().toString(),
                "path" to request.getDescription(false)
            )
        )
        return ResponseEntity.badRequest().body(errorResponse)
    }

    /**
     * 处理通用异常
     */
    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        ex: Exception,
        request: WebRequest
    ): ResponseEntity<Map<String, Any>> {
        logger.error("未处理的异常: {}", ex.message, ex)
        
        val errorResponse = mapOf(
            "success" to false,
            "error" to mapOf(
                "code" to "INTERNAL_ERROR",
                "message" to "系统内部错误",
                "details" to "请联系系统管理员",
                "timestamp" to LocalDateTime.now().toString(),
                "path" to request.getDescription(false)
            )
        )
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }
}
