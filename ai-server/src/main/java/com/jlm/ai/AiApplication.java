package com.jlm.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class AiApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiApplication.class, args);
        System.out.println("====== AI微服务启动成功 ======");
    }
}