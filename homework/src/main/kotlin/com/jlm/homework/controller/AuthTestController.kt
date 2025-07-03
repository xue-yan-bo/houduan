package com.jlm.homework.controller

import com.jlm.homework.service.StudentFeignService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import jakarta.servlet.http.HttpServletRequest

/**
 * 认证测试控制器
 * 演示如何透传认证信息到学生服务
 */
@RestController
@RequestMapping("/api/auth-test")
class AuthTestController(
    private val studentFeignService: StudentFeignService
) {

    /**
     * 测试带认证的学生列表获取
     * 前端需要在请求头中包含 Authorization 或 Admin-Token
     * 
     * 示例请求：
     * curl -H "Authorization: Bearer your-token" http://localhost:18080/api/auth-test/students
     * 或
     * curl -H "Admin-Token: your-admin-token" http://localhost:18080/api/auth-test/students
     */
    @GetMapping("/students")
    fun getStudentsWithAuth(
        @RequestParam(defaultValue = "1") pageNum: Int,
        @RequestParam(defaultValue = "10") pageSize: Int,
        request: HttpServletRequest
    ): ResponseEntity<Any> {
        
        // 显示接收到的认证信息（用于调试）
        val authHeaders = mutableMapOf<String, String?>()
        authHeaders["Authorization"] = request.getHeader("Authorization")
        authHeaders["Admin-Token"] = request.getHeader("Admin-Token")
        authHeaders["Bearer"] = request.getHeader("Bearer")
        
        val studentList = studentFeignService.getStudentList(pageNum, pageSize)
        
        return ResponseEntity.ok(mapOf(
            "success" to (studentList != null),
            "data" to studentList,
            "message" to if (studentList != null) "获取学生列表成功" else "获取学生列表失败",
            "authInfo" to mapOf(
                "receivedHeaders" to authHeaders.filterValues { !it.isNullOrEmpty() },
                "note" to "认证信息已透传给学生服务"
            )
        ))
    }

    /**
     * 测试带认证的单个学生获取
     */
    @GetMapping("/students/{id}")
    fun getStudentWithAuth(
        @PathVariable id: String,
        request: HttpServletRequest
    ): ResponseEntity<Any> {
        
        val authHeaders = mutableMapOf<String, String?>()
        authHeaders["Authorization"] = request.getHeader("Authorization")
        authHeaders["Admin-Token"] = request.getHeader("Admin-Token")
        
        val student = studentFeignService.getStudentById(id)
        
        return ResponseEntity.ok(mapOf(
            "success" to (student != null),
            "data" to student,
            "message" to if (student != null) "获取学生信息成功" else "获取学生信息失败",
            "authInfo" to mapOf(
                "receivedHeaders" to authHeaders.filterValues { !it.isNullOrEmpty() },
                "studentId" to id
            )
        ))
    }

    /**
     * 检查认证状态
     */
    @GetMapping("/auth-status")
    fun checkAuthStatus(request: HttpServletRequest): ResponseEntity<Any> {
        
        val authHeaders = mutableMapOf<String, String?>()
        authHeaders["Authorization"] = request.getHeader("Authorization")
        authHeaders["Admin-Token"] = request.getHeader("Admin-Token")
        authHeaders["Bearer"] = request.getHeader("Bearer")
        authHeaders["User-Agent"] = request.getHeader("User-Agent")
        
        val hasAuth = authHeaders.values.any { !it.isNullOrEmpty() }
        
        return ResponseEntity.ok(mapOf(
            "success" to true,
            "data" to mapOf(
                "hasAuthentication" to hasAuth,
                "headers" to authHeaders.filterValues { !it.isNullOrEmpty() },
                "remoteAddr" to request.remoteAddr,
                "requestURI" to request.requestURI
            ),
            "message" to if (hasAuth) "检测到认证信息" else "未检测到认证信息",
            "instructions" to listOf(
                "请在请求头中添加认证信息：",
                "Authorization: Bearer your-token",
                "或 Admin-Token: your-admin-token"
            )
        ))
    }

    /**
     * 模拟前端调用的完整示例
     */
    @PostMapping("/simulate-frontend")
    fun simulateFrontendCall(
        @RequestHeader(value = "Authorization", required = false) authorization: String?,
        @RequestHeader(value = "Admin-Token", required = false) adminToken: String?,
        @RequestBody(required = false) requestBody: Map<String, Any>?
    ): ResponseEntity<Any> {
        
        val authInfo = mutableMapOf<String, String?>()
        if (!authorization.isNullOrEmpty()) authInfo["Authorization"] = authorization
        if (!adminToken.isNullOrEmpty()) authInfo["Admin-Token"] = adminToken
        
        // 调用学生服务
        val studentList = studentFeignService.getStudentList(1, 5)
        
        return ResponseEntity.ok(mapOf(
            "success" to true,
            "data" to mapOf(
                "studentList" to studentList,
                "requestBody" to requestBody,
                "authUsed" to authInfo
            ),
            "message" to "模拟前端调用完成",
            "flow" to listOf(
                "1. 前端发送请求到 homework 服务",
                "2. homework 服务透传认证信息",
                "3. 调用 jlm-student 服务",
                "4. 返回结果给前端"
            )
        ))
    }

    /**
     * 获取使用说明
     */
    @GetMapping("/usage")
    fun getUsage(): ResponseEntity<Any> {
        return ResponseEntity.ok(mapOf(
            "success" to true,
            "data" to mapOf(
                "title" to "OpenFeign 认证透传使用说明",
                "description" to "本服务会自动透传前端请求中的认证信息到学生服务",
                "supportedHeaders" to listOf(
                    "Authorization: Bearer token",
                    "Admin-Token: admin-token",
                    "Bearer: token"
                ),
                "examples" to mapOf(
                    "curl" to listOf(
                        "curl -H \"Authorization: Bearer your-token\" http://localhost:18080/api/auth-test/students",
                        "curl -H \"Admin-Token: your-admin-token\" http://localhost:18080/api/auth-test/students/123"
                    ),
                    "javascript" to mapOf(
                        "fetch" to """
                            fetch('/api/auth-test/students', {
                                headers: {
                                    'Authorization': 'Bearer your-token',
                                    'Content-Type': 'application/json'
                                }
                            })
                        """.trimIndent()
                    )
                ),
                "flow" to listOf(
                    "前端 → homework 服务（带认证头）",
                    "homework 服务 → jlm-student 服务（透传认证头）",
                    "jlm-student 服务 → 验证认证并返回数据",
                    "homework 服务 → 前端（返回结果）"
                )
            ),
            "message" to "认证透传功能已启用"
        ))
    }
}
