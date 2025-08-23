package com.jlm.remote.example

import com.jlm.remote.service.NacosRemoteService
import kotlinx.coroutines.runBlocking
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.context.annotation.Bean

/**
 * Nacos示例应用程序
 * 演示如何在Spring Boot应用中使用Remote Server的Nacos功能
 * 
 * 启动前请确保：
 * 1. Nacos服务器正在运行
 * 2. 配置文件中正确设置了Nacos地址和服务映射
 * 3. 目标服务已注册到Nacos
 */
@SpringBootApplication
@EnableDiscoveryClient
class NacosExampleApplication {

    @Bean
    fun nacosExampleRunner(
        nacosRemoteService: NacosRemoteService,
        nacosServiceExample: NacosServiceExample
    ) = CommandLineRunner {
        println("\n" + "=".repeat(60))
        println("Remote Server Nacos 功能演示开始")
        println("=".repeat(60))
        
        // 运行所有示例
        nacosServiceExample.runAllExamples()
        
        println("\n" + "=".repeat(60))
        println("演示完成")
        println("=".repeat(60))
    }
}

/**
 * 主函数 - 用于独立运行示例
 */
fun main(args: Array<String>) {
    // 设置必要的系统属性（如果没有在配置文件中设置）
    System.setProperty("spring.profiles.active", "nacos-example")
    
    val context = SpringApplication.run(NacosExampleApplication::class.java, *args)
    
    // 可以在这里添加额外的演示代码
    val nacosRemoteService = context.getBean(NacosRemoteService::class.java)
    
    runBlocking {
        println("\n== 额外的演示代码 ==")
        
        // 演示服务发现
        val services = listOf("user-service", "order-service", "product-service")
        services.forEach { serviceAlias ->
            val isAvailable = nacosRemoteService.isServiceAvailable(serviceAlias)
            val instances = nacosRemoteService.getServiceInstances(serviceAlias)
            println("服务 $serviceAlias: 可用=$isAvailable, 实例数=${instances.size}")
        }
        
        // 演示简单的API调用
        try {
            val healthCheck = nacosRemoteService.getByServiceName(
                serviceAlias = "user-service",
                path = "/actuator/health"
            )
            println("用户服务健康检查: $healthCheck")
        } catch (e: Exception) {
            println("健康检查失败: ${e.message}")
        }
    }
    
    // 保持应用运行一段时间以便观察日志
    Thread.sleep(5000)
    context.close()
}
