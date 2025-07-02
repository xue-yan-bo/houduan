package com.jlm.homework

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient

/**
 * Homework微服务主启动类
 * 集成Nacos服务注册与发现功能
 */
@SpringBootApplication
@EnableDiscoveryClient  // 启用服务发现客户端
class HomeworkApplication

fun main(args: Array<String>) {
	runApplication<HomeworkApplication>(*args)
}
