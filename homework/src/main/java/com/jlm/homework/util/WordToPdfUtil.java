package com.jlm.homework.util;

import com.jlm.homework.config.MinioConfig;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Word文档转PDF工具类 - 使用DocToPDF服务
 */
@Slf4j
@Component
public class WordToPdfUtil {

    private final DocToPDF docToPDF;

    @Autowired
    public WordToPdfUtil(MinioClient minioClient, MinioConfig minioConfig, WebClient.Builder webClientBuilder, 
                       @Value("${pdf.path:/tmp/pdf}") String pdfPath, 
                       @Value("${minio.bucketName:exercise}") String bucketName,
                       @Value("${stirling.api.base-url:http://192.168.1.156:8089}") String stirlingBaseUrl) {
        // 初始化DocToPDF
        WebClient stirlingWebClient = webClientBuilder.baseUrl(stirlingBaseUrl).build();
        this.docToPDF = new DocToPDF(stirlingWebClient, pdfPath, minioClient, minioConfig, bucketName);
        
        log.info("WordToPdfUtil初始化成功");
        log.info("Stirling API base URL: {}", stirlingBaseUrl);
        log.info("MinIO内网endpoint: {}, 公网endpoint: {}", minioConfig.getEndpoint(), minioConfig.getPublicEndpoint());
    }

    /**
     * 转换MinIO中的Word文档为PDF - 调用DocToPDF的方法
     */
    public String convertMinioWordToPdf(String wordUrl) throws Exception {
        log.info("开始处理MinIO中的Word文件: {}", wordUrl);
        
        // 调用DocToPDF的wordToPdf方法
        String pdfUrl = docToPDF.wordToPdf(wordUrl);
        
        log.info("PDF文件转换并上传成功，URL: {}", pdfUrl);
        return pdfUrl;
    }

    /**
     * 转换本地Word文档为PDF - 调用DocToPDF的方法
     */
    public String convertLocalWordToPdf(String wordPath) throws Exception {
        log.info("开始处理本地Word文件: {}", wordPath);
        
        // 调用DocToPDF的wordToPdfLocal方法
        String pdfUrl = docToPDF.wordToPdfLocal(wordPath);
        
        log.info("PDF文件转换并上传成功，URL: {}", pdfUrl);
        return pdfUrl;
    }
}
