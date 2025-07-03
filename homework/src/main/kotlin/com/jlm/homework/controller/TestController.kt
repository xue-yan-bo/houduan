package com.jlm.homework.controller

import com.jlm.homework.config.ResponseVO
import com.jlm.homework.exception.*
import org.springframework.web.bind.annotation.*

/**
 * 测试控制器
 * 演示统一结果封装和异常处理功能
 */
@RestController
@RequestMapping("/api/test")
class TestController {

    /**
     * 测试成功响应 - 返回字符串
     * GET /api/test/success-string
     */
    @GetMapping("/success-string")
    fun testSuccessString(): String {
        return "测试成功，返回字符串"
    }

    /**
     * 测试成功响应 - 返回对象
     * GET /api/test/success-object
     */
    @GetMapping("/success-object")
    fun testSuccessObject(): Map<String, Any> {
        return mapOf(
            "id" to 1,
            "name" to "测试用户",
            "email" to "test@example.com",
            "age" to 25
        )
    }

    /**
     * 测试成功响应 - 返回列表
     * GET /api/test/success-list
     */
    @GetMapping("/success-list")
    fun testSuccessList(): List<Map<String, Any>> {
        return listOf(
            mapOf("id" to 1, "name" to "张三"),
            mapOf("id" to 2, "name" to "李四"),
            mapOf("id" to 3, "name" to "王五")
        )
    }

    /**
     * 测试直接返回ResponseVO
     * GET /api/test/direct-response
     */
    @GetMapping("/direct-response")
    fun testDirectResponse(): ResponseVO<String> {
        return ResponseVO.success("直接返回ResponseVO", "自定义成功消息")
    }

    /**
     * 测试参数验证异常
     * GET /api/test/parameter-error?name=
     */
    @GetMapping("/parameter-error")
    fun testParameterError(@RequestParam name: String): String {
        if (name.isBlank()) {
            throw ParameterException("用户名不能为空")
        }
        return "Hello, $name"
    }

    /**
     * 测试资源未找到异常
     * GET /api/test/not-found/{id}
     */
    @GetMapping("/not-found/{id}")
    fun testNotFound(@PathVariable id: Long): Map<String, Any> {
        if (id <= 0) {
            throw ResourceNotFoundException("用户不存在，ID: $id")
        }
        return mapOf(
            "id" to id,
            "name" to "测试用户$id"
        )
    }

    /**
     * 测试权限不足异常
     * GET /api/test/permission-denied
     */
    @GetMapping("/permission-denied")
    fun testPermissionDenied(): String {
        throw PermissionDeniedException("您没有权限访问此资源")
    }

    /**
     * 测试服务不可用异常
     * GET /api/test/service-unavailable
     */
    @GetMapping("/service-unavailable")
    fun testServiceUnavailable(): String {
        throw ServiceUnavailableException("外部服务暂时不可用，请稍后重试")
    }

    /**
     * 测试数据重复异常
     * POST /api/test/duplicate-data
     */
    @PostMapping("/duplicate-data")
    fun testDuplicateData(@RequestBody request: Map<String, String>): String {
        val email = request["email"]
        if (email == "admin@example.com") {
            throw DuplicateDataException("邮箱地址已存在: $email")
        }
        return "用户创建成功"
    }

    /**
     * 测试数据状态异常
     * PUT /api/test/data-status/{id}
     */
    @PutMapping("/data-status/{id}")
    fun testDataStatus(@PathVariable id: Long): String {
        if (id == 999L) {
            throw DataStatusException("用户状态异常，无法执行此操作")
        }
        return "操作成功"
    }

    /**
     * 测试运行时异常
     * GET /api/test/runtime-error
     */
    @GetMapping("/runtime-error")
    fun testRuntimeError(): String {
        // 模拟运行时异常
        val numbers = listOf(1, 2, 3)
        return "第四个数字是: ${numbers[3]}" // 会抛出IndexOutOfBoundsException
    }

    /**
     * 测试空指针异常
     * GET /api/test/null-pointer
     */
    @GetMapping("/null-pointer")
    fun testNullPointer(): String {
        val nullString: String? = null
        return "字符串长度: ${nullString!!.length}" // 会抛出NullPointerException
    }

    /**
     * 测试非法参数异常
     * GET /api/test/illegal-argument?number=
     */
    @GetMapping("/illegal-argument")
    fun testIllegalArgument(@RequestParam number: String): String {
        val num = number.toIntOrNull() 
            ?: throw IllegalArgumentException("参数必须是有效的数字: $number")
        
        if (num < 0) {
            throw IllegalArgumentException("数字不能为负数: $num")
        }
        
        return "您输入的数字是: $num"
    }

    /**
     * 测试自定义业务异常
     * GET /api/test/business-error?code=500
     */
    @GetMapping("/business-error")
    fun testBusinessError(@RequestParam(defaultValue = "400") code: Int): String {
        when (code) {
            400 -> throw BusinessException(400, "自定义400错误")
            401 -> throw BusinessException(401, "自定义401错误")
            403 -> throw BusinessException(403, "自定义403错误")
            404 -> throw BusinessException(404, "自定义404错误")
            500 -> throw BusinessException(500, "自定义500错误")
            else -> return "没有对应的错误码测试"
        }
    }

    /**
     * 测试复杂的业务逻辑
     * POST /api/test/complex-business
     */
    @PostMapping("/complex-business")
    fun testComplexBusiness(@RequestBody request: Map<String, Any>): Map<String, Any> {
        val name = request["name"] as? String
        val age = request["age"] as? Int
        val email = request["email"] as? String

        // 参数验证
        if (name.isNullOrBlank()) {
            throw ParameterException("姓名不能为空")
        }
        
        if (age == null || age < 0 || age > 150) {
            throw ParameterException("年龄必须在0-150之间")
        }
        
        if (email.isNullOrBlank() || !email.contains("@")) {
            throw ParameterException("邮箱格式不正确")
        }

        // 业务逻辑验证
        if (email == "banned@example.com") {
            throw PermissionDeniedException("该邮箱已被禁用")
        }
        
        if (name == "重复用户") {
            throw DuplicateDataException("用户名已存在")
        }

        // 模拟成功场景
        return mapOf(
            "id" to System.currentTimeMillis(),
            "name" to name,
            "age" to age,
            "email" to email,
            "status" to "ACTIVE",
            "createdAt" to System.currentTimeMillis()
        )
    }

    /**
     * 健康检查接口
     * GET /api/test/health
     */
    @GetMapping("/health")
    fun health(): Map<String, Any> {
        return mapOf(
            "status" to "UP",
            "service" to "test-controller",
            "timestamp" to System.currentTimeMillis(),
            "message" to "测试控制器运行正常"
        )
    }
} 