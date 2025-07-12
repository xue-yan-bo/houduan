package com.jlm.homework.config

import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.transaction.annotation.EnableTransactionManagement

/**
 * 数据库配置类
 * 配置JPA Repository和实体扫描
 */
@Configuration
@EnableJpaRepositories(
    basePackages = ["com.jlm.homework.repository", "com.jlm.homework.mapper"]
)
@EntityScan(
    basePackages = ["com.jlm.homework.entity"]
)
@EnableTransactionManagement
class DatabaseConfig {
    
    // 这里可以添加自定义的数据库配置Bean
    // 例如：自定义的数据源、事务管理器等
}
