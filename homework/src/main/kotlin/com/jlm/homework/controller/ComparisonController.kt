package com.jlm.homework.controller

import com.jlm.homework.service.StudentFeignService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import kotlin.system.measureTimeMillis

/**
 * OpenFeign 功能演示控制器
 * 展示 OpenFeign 远程调用的功能和优势
 */
@RestController
@RequestMapping("/api/comparison")
class ComparisonController {

    @Autowired
    private lateinit var studentFeignService: StudentFeignService

    /**
     * 测试 OpenFeign 获取学生信息的性能
     * GET /api/comparison/student/{id}
     */
    @GetMapping("/student/{id}")
    fun testGetStudent(@PathVariable id: String): ResponseEntity<Any> {
        val result = mutableMapOf<String, Any>()

        // OpenFeign 方式
        val feignTime = measureTimeMillis {
            try {
                val feignResult = studentFeignService.getStudentById(id)
                result["feign"] = mapOf(
                    "success" to (feignResult != null),
                    "data" to feignResult,
                    "method" to "OpenFeign",
                    "error" to null
                )
            } catch (e: Exception) {
                result["feign"] = mapOf(
                    "success" to false,
                    "data" to null,
                    "method" to "OpenFeign",
                    "error" to e.message
                )
            }
        }

        return ResponseEntity.ok(mapOf(
            "success" to true,
            "data" to mapOf(
                "studentId" to id,
                "result" to result,
                "performance" to mapOf(
                    "responseTime" to "${feignTime}ms"
                ),
                "features" to mapOf(
                    "typesSafety" to "强类型，编译时检查",
                    "codeStyle" to "声明式，接口定义",
                    "errorHandling" to "统一异常处理和降级",
                    "monitoring" to "内置监控和指标",
                    "loadBalancing" to "自动负载均衡",
                    "circuitBreaker" to "熔断器支持"
                )
            ),
            "message" to "OpenFeign 测试完成"
        ))
    }

    /**
     * OpenFeign 服务健康检查
     * GET /api/comparison/health
     */
    @GetMapping("/health")
    fun healthCheck(): ResponseEntity<Any> {
        val result = mutableMapOf<String, Any>()

        // OpenFeign 健康检查
        val responseTime = measureTimeMillis {
            try {
                val isAvailable = studentFeignService.isStudentServiceAvailable()
                val healthDetails = studentFeignService.getServiceHealthDetails()
                result["feign"] = mapOf(
                    "available" to isAvailable,
                    "details" to healthDetails,
                    "method" to "OpenFeign"
                )
            } catch (e: Exception) {
                result["feign"] = mapOf(
                    "available" to false,
                    "details" to mapOf("error" to e.message),
                    "method" to "OpenFeign"
                )
            }
        }

        return ResponseEntity.ok(mapOf(
            "success" to true,
            "data" to mapOf(
                "result" to result,
                "performance" to mapOf(
                    "responseTime" to "${responseTime}ms"
                ),
                "advantages" to listOf(
                    "标准化的健康检查",
                    "丰富的监控指标",
                    "熔断和降级机制",
                    "自动重试机制",
                    "负载均衡支持"
                )
            ),
            "message" to "OpenFeign 健康检查完成"
        ))
    }

    /**
     * OpenFeign 功能特性介绍
     * GET /api/comparison/features
     */
    @GetMapping("/features")
    fun getOpenFeignFeatures(): ResponseEntity<Any> {
        return ResponseEntity.ok(mapOf(
            "success" to true,
            "data" to mapOf(
                "openFeign" to mapOf(
                    "description" to "Spring Cloud 官方推荐的声明式HTTP客户端",
                    "coreFeatures" to listOf(
                        "声明式编程模型",
                        "强类型安全",
                        "内置负载均衡",
                        "自动熔断降级",
                        "请求/响应拦截器",
                        "自动重试机制",
                        "服务发现集成",
                        "监控和指标集成"
                    ),
                    "advantages" to listOf(
                        "代码简洁易维护",
                        "类型安全，编译时检查",
                        "统一的错误处理",
                        "丰富的配置选项",
                        "完善的Spring生态集成",
                        "优秀的测试支持",
                        "活跃的社区支持"
                    ),
                    "bestPractices" to listOf(
                        "定义清晰的接口契约",
                        "实现合适的降级策略",
                        "配置合理的超时时间",
                        "使用熔断器保护系统",
                        "添加监控和日志",
                        "编写充分的单元测试"
                    ),
                    "useCase" to listOf(
                        "微服务间通信",
                        "第三方API调用",
                        "分布式系统集成",
                        "需要高可靠性的场景"
                    )
                ),
                "configuration" to mapOf(
                    "timeout" to "支持连接和读取超时配置",
                    "retry" to "支持自动重试和指数退避",
                    "circuitBreaker" to "集成Resilience4j熔断器",
                    "loadBalancer" to "自动负载均衡",
                    "compression" to "支持请求/响应压缩"
                ),
                "monitoring" to mapOf(
                    "metrics" to "自动收集调用指标",
                    "tracing" to "支持分布式链路追踪",
                    "logging" to "详细的请求/响应日志",
                    "health" to "健康检查集成"
                )
            ),
            "message" to "OpenFeign 功能特性介绍完成"
        ))
    }

    /**
     * OpenFeign 最佳实践指南
     * GET /api/comparison/best-practices
     */
    @GetMapping("/best-practices")
    fun getBestPractices(): ResponseEntity<Any> {
        return ResponseEntity.ok(mapOf(
            "success" to true,
            "data" to mapOf(
                "currentStatus" to mapOf(
                    "feignEnabled" to true,
                    "recommendation" to "已使用 OpenFeign，继续优化配置"
                ),
                "implementationGuide" to mapOf(
                    "step1" to mapOf(
                        "title" to "接口设计",
                        "tasks" to listOf(
                            "定义清晰的接口契约",
                            "使用合适的HTTP方法",
                            "设计合理的请求/响应结构",
                            "添加详细的接口文档"
                        )
                    ),
                    "step2" to mapOf(
                        "title" to "配置优化",
                        "tasks" to listOf(
                            "配置合适的超时时间",
                            "启用请求/响应压缩",
                            "配置重试策略",
                            "设置连接池参数"
                        )
                    ),
                    "step3" to mapOf(
                        "title" to "容错处理",
                        "tasks" to listOf(
                            "实现熔断器",
                            "设计降级策略",
                            "添加重试机制",
                            "处理异常情况"
                        )
                    ),
                    "step4" to mapOf(
                        "title" to "监控运维",
                        "tasks" to listOf(
                            "添加监控指标",
                            "配置日志记录",
                            "集成链路追踪",
                            "设置告警规则"
                        )
                    )
                ),
                "bestPractices" to listOf(
                    "使用强类型接口定义",
                    "实现统一的错误处理",
                    "配置合理的超时和重试",
                    "实现优雅的降级策略",
                    "添加完善的监控和日志",
                    "编写充分的单元测试",
                    "定期检查和优化配置",
                    "保持接口版本兼容性"
                ),
                "commonPitfalls" to listOf(
                    "超时时间设置不当",
                    "缺少降级处理",
                    "忽略异常处理",
                    "监控指标不足",
                    "测试覆盖率低"
                )
            ),
            "message" to "OpenFeign 最佳实践指南获取成功"
        ))
    }
}
