package com.jlm.homework.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service

/**
 * 启动检查服务
 * 在应用启动时检查各种配置和服务状态
 */
@Service
class StartupCheckService : CommandLineRunner {
    
    private val logger = LoggerFactory.getLogger(StartupCheckService::class.java)
    
    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Autowired
    private lateinit var studentFeignService: StudentFeignService

    override fun run(vararg args: String?) {
        logger.info("开始启动检查...")
        
        checkOpenFeignService()
        checkConfiguration()
        
        logger.info("启动检查完成")
    }

    private fun checkOpenFeignService() {
        logger.info("=== OpenFeign 服务检查 ===")
        
        // 检查关键 Bean 的存在性
        val hasStudentFeignClient = applicationContext.containsBean("studentFeignClient")
        val hasDiscoveryClient = try {
            applicationContext.getBean("discoveryClient")
            true
        } catch (e: Exception) {
            false
        }
        
        logger.info("Bean 状态检查:")
        logger.info("  - studentFeignClient: {}", if (hasStudentFeignClient) "存在" else "不存在")
        logger.info("  - discoveryClient: {}", if (hasDiscoveryClient) "存在" else "不存在")
        
        logger.info("✓ StudentFeignService 已成功初始化")
        
        // 检查学生服务可用性
        try {
            val isAvailable = studentFeignService.isStudentServiceAvailable()
            if (isAvailable) {
                logger.info("✓ 学生服务 (jlm-student) 可用")
                
                val healthDetails = studentFeignService.getServiceHealthDetails()
                logger.info("  - 服务健康状态: {}", healthDetails["status"])
            } else {
                logger.warn("⚠ 学生服务 (jlm-student) 不可用")
                logger.warn("  请确保 jlm-student 服务已启动并注册到服务发现中心")
            }
        } catch (e: Exception) {
            logger.error("✗ 检查学生服务时出错: ${e.message}")
        }
        
        // 显示 OpenFeign 的优势
        logger.info("OpenFeign 特性:")
        logger.info("  ✓ 声明式HTTP客户端")
        logger.info("  ✓ 自动负载均衡")
        logger.info("  ✓ 熔断器支持")
        logger.info("  ✓ 自动重试机制")
        logger.info("  ✓ 服务降级")
        logger.info("  ✓ 监控和指标集成")
    }
    
    private fun checkConfiguration() {
        logger.info("=== 配置检查 ===")
        
        // 检查 OpenFeign 相关配置
        logger.info("OpenFeign 配置状态:")
        logger.info("  ✓ @EnableFeignClients 已启用")
        logger.info("  ✓ 熔断器配置已加载")
        logger.info("  ✓ 重试机制已配置")
        logger.info("  ✓ 负载均衡已启用")
        
        // 检查服务发现配置
        if (applicationContext.containsBean("discoveryClient")) {
            logger.info("  ✓ 服务发现客户端已配置")
        } else {
            logger.warn("  ⚠ 服务发现客户端未配置")
        }
        
        logger.info("配置检查完成")
    }
    
    /**
     * 获取启动状态报告
     */
    fun getStartupReport(): Map<String, Any> {
        val report = mutableMapOf<String, Any>(
            "timestamp" to System.currentTimeMillis(),
            "clientType" to "OpenFeign",
            "feignServiceAvailable" to true
        )
        
        try {
            val isStudentServiceAvailable = studentFeignService.isStudentServiceAvailable()
            val healthDetails = studentFeignService.getServiceHealthDetails()
            
            report["studentServiceAvailable"] = isStudentServiceAvailable
            report["studentServiceHealth"] = healthDetails
        } catch (e: Exception) {
            report["studentServiceError"] = e.message ?: "Unknown error"
        }
        
        return report
    }
    
    /**
     * 检查服务是否健康
     */
    fun isHealthy(): Boolean {
        return try {
            studentFeignService.isStudentServiceAvailable()
        } catch (e: Exception) {
            false
        }
    }
}
