package com.jlm.homework.controller

import com.jlm.homework.feign.CreateStudentRequest
import com.jlm.homework.feign.UpdateStudentRequest
import com.jlm.homework.service.StudentFeignService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag

/**
 * 学生控制器
 * 提供学生相关的REST接口，使用OpenFeign调用jlm-student服务
 */
@Tag(name = "学生服务", description = "通过OpenFeign调用jlm-student服务，提供学生信息的代理访问")
@RestController
@RequestMapping("/api/student")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "adminToken")
class StudentController(
    private val studentFeignService: StudentFeignService
) {

    /**
     * 根据ID获取学生信息
     * GET /api/student/{id}
     */
    @GetMapping("/{id}")
    fun getStudentById(@PathVariable id: String): ResponseEntity<Any> {
        val student = studentFeignService.getStudentById(id)
        
        return if (student != null) {
            ResponseEntity.ok(mapOf(
                "success" to true,
                "data" to student,
                "message" to "获取学生信息成功"
            ))
        } else {
            ResponseEntity.ok(mapOf(
                "success" to false,
                "data" to null,
                "message" to "未找到学生信息或服务不可用"
            ))
        }
    }

    /**
     * 获取学生列表
     * GET /api/student?page=1&size=10
     */
    @GetMapping
    fun getStudentList(
        @RequestParam(defaultValue = "1") pageNum: Int,
        @RequestParam(defaultValue = "10") pageSize: Int
    ): ResponseEntity<Any> {
        val studentList = studentFeignService.getStudentList(pageNum, pageSize)
        
        return if (studentList != null) {
            ResponseEntity.ok(mapOf(
                "success" to true,
                "data" to studentList,
                "message" to "获取学生列表成功"
            ))
        } else {
            ResponseEntity.ok(mapOf(
                "success" to false,
                "data" to null,
                "message" to "获取学生列表失败或服务不可用"
            ))
        }
    }

    /**
     * 创建学生
     * POST /api/student
     */
    @PostMapping
    fun createStudent(@RequestBody request: CreateStudentRequest): ResponseEntity<Any> {
        val student = studentFeignService.createStudent(request)
        
        return if (student != null) {
            ResponseEntity.ok(mapOf(
                "success" to true,
                "data" to student,
                "message" to "创建学生成功"
            ))
        } else {
            ResponseEntity.ok(mapOf(
                "success" to false,
                "data" to null,
                "message" to "创建学生失败或服务不可用"
            ))
        }
    }

    /**
     * 更新学生信息
     * PUT /api/student/{id}
     */
    @PutMapping("/{id}")
    fun updateStudent(
        @PathVariable id: String,
        @RequestBody request: UpdateStudentRequest
    ): ResponseEntity<Any> {
        val student = studentFeignService.updateStudent(id, request)

        return if (student != null) {
            ResponseEntity.ok(mapOf(
                "success" to true,
                "data" to student,
                "message" to "更新学生信息成功"
            ))
        } else {
            ResponseEntity.ok(mapOf(
                "success" to false,
                "data" to null,
                "message" to "更新学生信息失败或服务不可用"
            ))
        }
    }

    /**
     * 删除学生
     * DELETE /api/student/{id}
     */
    @DeleteMapping("/{id}")
    fun deleteStudent(@PathVariable id: String): ResponseEntity<Any> {
        val success = studentFeignService.deleteStudent(id)

        return ResponseEntity.ok(mapOf(
            "success" to success,
            "data" to null,
            "message" to if (success) "删除学生成功" else "删除学生失败或服务不可用"
        ))
    }

    /**
     * 批量获取学生信息
     * POST /api/student/batch
     */
    @PostMapping("/batch")
    fun getStudentsByIds(@RequestBody studentIds: List<String>): ResponseEntity<Any> {
        val students = studentFeignService.getStudentsByIds(studentIds)

        return ResponseEntity.ok(mapOf(
            "success" to true,
            "data" to mapOf(
                "students" to students,
                "total" to students.size,
                "requested" to studentIds.size
            ),
            "message" to "批量获取学生信息完成"
        ))
    }

    /**
     * 搜索学生
     * GET /api/student/search?keyword=xxx&page=1&size=10
     */
    @GetMapping("/search")
    fun searchStudents(
        @RequestParam keyword: String,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<Any> {
        val searchResult = studentFeignService.searchStudents(keyword, page, size)

        return if (searchResult != null) {
            ResponseEntity.ok(mapOf(
                "success" to true,
                "data" to searchResult,
                "message" to "搜索学生完成"
            ))
        } else {
            ResponseEntity.ok(mapOf(
                "success" to false,
                "data" to null,
                "message" to "搜索学生失败或服务不可用"
            ))
        }
    }

    /**
     * 检查学生服务状态
     * GET /api/student/service/status
     */
    @GetMapping("/service/status")
    fun getServiceStatus(): ResponseEntity<Any> {
        val isAvailable = studentFeignService.isStudentServiceAvailable()
        val healthDetails = studentFeignService.getServiceHealthDetails()

        return ResponseEntity.ok(mapOf(
            "success" to true,
            "data" to mapOf(
                "available" to isAvailable,
                "healthDetails" to healthDetails,
                "serviceType" to "OpenFeign",
                "timestamp" to System.currentTimeMillis()
            ),
            "message" to if (isAvailable) "学生服务可用" else "学生服务不可用"
        ))
    }

    /**
     * 健康检查接口
     * GET /api/student/health
     */
    @GetMapping("/health")
    fun health(): ResponseEntity<Any> {
        val isServiceAvailable = studentFeignService.isStudentServiceAvailable()

        return ResponseEntity.ok(mapOf(
            "success" to true,
            "data" to mapOf(
                "status" to "UP",
                "service" to "homework-student-openfeign-proxy",
                "studentServiceAvailable" to isServiceAvailable,
                "clientType" to "OpenFeign",
                "timestamp" to System.currentTimeMillis()
            ),
            "message" to "基于OpenFeign的学生代理服务正常"
        ))
    }

    /**
     * 配置检查接口
     * GET /api/student/config
     */
    @GetMapping("/config")
    fun getConfig(): ResponseEntity<Any> {
        return ResponseEntity.ok(mapOf(
            "success" to true,
            "data" to mapOf(
                "clientType" to "OpenFeign",
                "targetService" to "jlm-student",
                "features" to listOf(
                    "声明式HTTP客户端",
                    "自动负载均衡",
                    "熔断器支持",
                    "重试机制",
                    "请求/响应拦截器",
                    "服务降级"
                ),
                "advantages" to listOf(
                    "Spring Cloud 标准解决方案",
                    "更好的类型安全",
                    "更简洁的代码",
                    "更强的可维护性",
                    "更完善的监控和治理"
                )
            ),
            "message" to "OpenFeign配置信息获取成功"
        ))
    }
}
