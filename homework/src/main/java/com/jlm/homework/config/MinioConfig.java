package com.jlm.homework.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author QingYang
 * @version 1.0
 * @description
 * @date 2023/3/3 0003
 */
@Configuration
public class MinioConfig {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.publicEndpoint}")
    private String publicEndpoint;

    @Value("${minio.accessKey}")
    private String accessKey;

    @Value("${minio.secretKey}")
    private String secretKey;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    @Bean
    public String minioPublicEndpoint() {
        return publicEndpoint;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getPublicEndpoint() {
        return publicEndpoint;
    }
}
