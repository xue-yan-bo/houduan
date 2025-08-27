package com.jlm.socketserver.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 数据库配置类
 * 配置JPA Repository和实体扫描
 */
@Configuration
@EnableJpaRepositories(
        basePackages = {"com.jlm.socketserver.repository"}
        )
@EntityScan(
        basePackages = {"com.jlm.socketserver.entity"}
        )
@EnableTransactionManagement
public class DatabaseConfig {
}
