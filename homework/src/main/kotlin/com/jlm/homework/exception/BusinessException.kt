package com.jlm.homework.exception

/**
 * 业务异常基类
 * 用于封装业务逻辑中的异常情况
 */
open class BusinessException(
    /**
     * 错误代码
     */
    val code: Int,
    
    /**
     * 错误消息
     */
    override val message: String,
    
    /**
     * 异常原因
     */
    cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * 参数验证异常
 */
class ParameterException(
    message: String = "参数错误",
    cause: Throwable? = null
) : BusinessException(400, message, cause)

/**
 * 资源未找到异常
 */
class ResourceNotFoundException(
    message: String = "资源未找到",
    cause: Throwable? = null
) : BusinessException(404, message, cause)

/**
 * 权限不足异常
 */
class PermissionDeniedException(
    message: String = "权限不足",
    cause: Throwable? = null
) : BusinessException(403, message, cause)

/**
 * 服务不可用异常
 */
class ServiceUnavailableException(
    message: String = "服务暂时不可用",
    cause: Throwable? = null
) : BusinessException(503, message, cause)

/**
 * 数据重复异常
 */
class DuplicateDataException(
    message: String = "数据已存在",
    cause: Throwable? = null
) : BusinessException(409, message, cause)

/**
 * 数据状态异常
 */
class DataStatusException(
    message: String = "数据状态异常",
    cause: Throwable? = null
) : BusinessException(422, message, cause) 