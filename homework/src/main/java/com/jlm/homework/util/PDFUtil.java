package com.jlm.homework.util;

import com.jlm.homework.config.MinioConfig;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

/**
 * PDF 工具类 - 提供 PDF 文件相关操作
 */
@Slf4j
@Component
public class PDFUtil {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    @Autowired
    public PDFUtil(MinioClient minioClient, MinioConfig minioConfig) {
        this.minioClient = minioClient;
        this.minioConfig = minioConfig;
    }

    /**
     * 获取 PDF 文件的页数
     * @param pdfUrl PDF 文件的 URL
     * @return PDF 文件的页数，如果获取失败则返回 1
     */
    public int getPdfPageCount(String pdfUrl) {
        if (pdfUrl == null || !pdfUrl.endsWith(".pdf")) {
            return 1;
        }

        File tempPdfFile = null;
        try {
            tempPdfFile = File.createTempFile("temp", ".pdf");
            
            // 从 MinIO 下载文件
            downloadFileFromMinio(pdfUrl, tempPdfFile);
            
            // 使用 PDFBox 解析 PDF 文件并获取页数
            try (PDDocument document = PDDocument.load(tempPdfFile)) {
                int pageCount = document.getNumberOfPages();
                log.info("PDF文件页数: {}，URL: {}", pageCount, pdfUrl);
                return pageCount;
            }
        } catch (Exception e) {
            log.warn("获取PDF页数失败: {}，URL: {}", e.getMessage(), pdfUrl);
            return 1;
        } finally {
            if (tempPdfFile != null && tempPdfFile.exists()) {
                tempPdfFile.delete();
            }
        }
    }

    /**
     * 从 MinIO 下载文件
     */
    private void downloadFileFromMinio(String fileUrl, File tempFile) throws Exception {
        try {
            String[] bucketAndPath = extractBucketAndObjectPathFromUrl(fileUrl);
            String bucketName = bucketAndPath[0];
            String objectPath = bucketAndPath[1];

            if (objectPath == null || objectPath.isEmpty()) {
                throw new IllegalArgumentException("无法从URL中提取对象路径: " + fileUrl);
            }

            // 尝试使用内网下载
            try {
                downloadFileFromMinioWithBucket(bucketName, objectPath, tempFile);
                log.info("内网下载PDF文件成功，大小: {} bytes", tempFile.length());
                return;
            } catch (Exception e) {
                log.warn("内网下载失败: {}，尝试使用公网下载", e.getMessage());
            }

            // 尝试使用公网下载
            String publicEndpoint = minioConfig.getPublicEndpoint();
            if (publicEndpoint != null && !publicEndpoint.isEmpty()) {
                log.info("尝试使用公网地址下载: {}", publicEndpoint);
                downloadFileFromMinioWithEndpoint(publicEndpoint, bucketName, objectPath, tempFile);
                log.info("公网下载PDF文件成功，大小: {} bytes", tempFile.length());
            } else {
                throw new RuntimeException("公网地址未配置，无法下载文件");
            }
        } catch (Exception e) {
            log.error("下载PDF文件失败: {}", e.getMessage());
            throw e;
        }
    }

    private void downloadFileFromMinioWithBucket(String bucketName, String objectPath, File tempFile) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(tempFile);
             InputStream is = minioClient.getObject(
                     GetObjectArgs.builder()
                             .bucket(bucketName)
                             .object(objectPath)
                             .build())) {
            byte[] buffer = new byte[1024 * 1024];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }
    }

    private void downloadFileFromMinioWithEndpoint(String endpoint, String bucketName, String objectPath, File tempFile) throws Exception {
        MinioClient publicMinioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(minioConfig.getAccessKey(), minioConfig.getSecretKey())
                .build();

        try (FileOutputStream fos = new FileOutputStream(tempFile);
             InputStream is = publicMinioClient.getObject(
                     GetObjectArgs.builder()
                             .bucket(bucketName)
                             .object(objectPath)
                             .build())) {
            byte[] buffer = new byte[1024 * 1024];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }
    }

    private String[] extractBucketAndObjectPathFromUrl(String url) {
        try {
            java.net.URL parsedUrl = new java.net.URL(url);
            String path = parsedUrl.getPath();

            if (path.startsWith("/")) {
                path = path.substring(1);
            }

            int firstSlashIndex = path.indexOf('/');
            if (firstSlashIndex > 0) {
                String bucket = path.substring(0, firstSlashIndex);
                String objectPath = path.substring(firstSlashIndex + 1);
                return new String[]{bucket, objectPath};
            } else {
                return new String[]{path, ""};
            }
        } catch (Exception e) {
            log.error("解析MinIO URL失败: {}", url, e);
            throw new IllegalArgumentException("无效的MinIO URL格式: " + url);
        }
    }

    /**
     * 合并多个 PDF 文件为一个 PDF 文件
     * @param pdfUrls PDF 文件的 URL 列表
     * @param outputFileName 输出文件名（不含扩展名）
     * @return 合并后的 PDF 文件的 URL
     */
    public String mergePdfFiles(List<String> pdfUrls, String outputFileName) throws Exception {
        if (pdfUrls == null || pdfUrls.isEmpty()) {
            throw new IllegalArgumentException("PDF文件列表不能为空");
        }

        if (outputFileName == null || outputFileName.isEmpty()) {
            outputFileName = "merged_pdf";
        }

        // 创建临时文件用于存储合并后的 PDF
        File mergedPdfFile = File.createTempFile(outputFileName, ".pdf");
        PDFMergerUtility merger = new PDFMergerUtility();

        try {
            // 下载并合并每个 PDF 文件
            for (String pdfUrl : pdfUrls) {
                if (pdfUrl == null || !pdfUrl.endsWith(".pdf")) {
                    log.warn("跳过非PDF文件: {}", pdfUrl);
                    continue;
                }

                File tempPdfFile = File.createTempFile("temp", ".pdf");
                try {
                    // 从 MinIO 下载文件
                    downloadFileFromMinio(pdfUrl, tempPdfFile);
                    
                    // 添加到合并工具
                    merger.addSource(tempPdfFile);
                    log.info("添加PDF文件到合并列表: {}", pdfUrl);
                } finally {
                    // 临时文件会在合并完成后自动删除
                    tempPdfFile.deleteOnExit();
                }
            }

            // 设置合并后的输出文件
            merger.setDestinationFileName(mergedPdfFile.getAbsolutePath());
            
            // 执行合并
            merger.mergeDocuments(null);
            log.info("PDF文件合并完成，输出文件大小: {} bytes", mergedPdfFile.length());

            // 上传合并后的 PDF 文件到 MinIO
            return uploadMergedPdfToMinio(mergedPdfFile, outputFileName, pdfUrls.get(0));
        } catch (Exception e) {
            log.error("合并PDF文件失败: {}", e.getMessage(), e);
            throw e;
        } finally {
            if (mergedPdfFile != null && mergedPdfFile.exists()) {
                mergedPdfFile.delete();
            }
        }
    }

    /**
     * 上传合并后的 PDF 文件到 MinIO
     */
    private String uploadMergedPdfToMinio(File mergedPdfFile, String outputFileName, String firstPdfUrl) throws Exception {
        if (firstPdfUrl == null) {
            throw new RuntimeException("无法获取存储桶信息");
        }

        String[] bucketAndPath = extractBucketAndObjectPathFromUrl(firstPdfUrl);
        String bucketName = bucketAndPath[0];
        String objectPath = "merged_pdfs/" + outputFileName + ".pdf";

        // 上传文件到 MinIO
        try (java.io.FileInputStream fis = new java.io.FileInputStream(mergedPdfFile)) {
            minioClient.putObject(
                    io.minio.PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectPath)
                            .stream(fis, mergedPdfFile.length(), -1)
                            .contentType("application/pdf")
                            .build()
            );
        }

        // 构建合并后的 PDF 文件 URL
        String pdfUrl = minioConfig.getPublicEndpoint() + "/" + bucketName + "/" + objectPath;
        log.info("合并后的PDF文件上传成功，URL: {}", pdfUrl);
        return pdfUrl;
    }
}
