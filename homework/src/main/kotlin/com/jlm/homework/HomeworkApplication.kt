package com.jlm.homework

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.context.annotation.ComponentScan

/**
 * Homework微服务主启动类
 * 集成Nacos服务注册与发现功能
 * JPA配置在DatabaseConfig中处理
 */
@SpringBootApplication
@ComponentScan(basePackages = ["com.jlm.homework"])
@EnableDiscoveryClient  // 启用服务发现客户端
@EnableFeignClients(basePackages = ["com.jlm.homework.feign"])  // 启用Feign客户端
class HomeworkApplication

fun main(args: Array<String>) {
	runApplication<HomeworkApplication>(*args)
}
