package com.jlm.homework.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.UUID;
import com.jlm.homework.config.MinioConfig;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
@Slf4j
public class DocToPDF {

    private final WebClient stirlingWebClient;
    private final String pdfPath;
    private final MinioClient minioClient;
    private final MinioConfig minioConfig;
    private final String bucketName;

    public DocToPDF(WebClient stirlingWebClient, String pdfPath, MinioClient minioClient,MinioConfig minioConfig, String bucketName) {
        this.stirlingWebClient = stirlingWebClient;
        this.pdfPath = pdfPath;
        this.minioClient = minioClient;
        this.minioConfig = minioConfig;
        this.bucketName = bucketName;
    }

    private String documentToPDF(String documentPath) {
        FileSystemResource file = new FileSystemResource(documentPath);

        byte[] bytes = stirlingWebClient.post()
                .uri("/api/v1/convert/file/pdf")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData("fileInput", file))
                .retrieve()
                .bodyToMono(byte[].class)
                .block();

        if (bytes == null) {
            throw new RuntimeException("文档转换PDF失败,未获取到响应数据");
        }

        String localPDFPath = pdfPath + File.separator + UUID.randomUUID() + ".pdf";
        FileUtil.writeBytes(bytes, localPDFPath);
        String resultUrl = null;

        try (InputStream inputStream = FileUtil.getInputStream(new File(localPDFPath))) {
            String objectName = localPDFPath.split("/")[localPDFPath.split("/").length - 1];
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, inputStream.available(), -1)
                            .contentType("application/pdf")
                            .build()
            );
            // 构建返回的URL
            String publicEndpoint = minioConfig.getPublicEndpoint();
            if (publicEndpoint != null && !publicEndpoint.isEmpty()) {
                resultUrl = publicEndpoint + "/" + bucketName + "/" + objectName;
            } else {
                resultUrl = "http://localhost:9000/" + bucketName + "/" + objectName; // 默认地址
            }
        } catch (Exception e) {
            throw new RuntimeException("PDF上传到MinIO失败: " + e.getMessage(), e);
        } finally {
            // 无论上传是否成功，都删除本地文件
            FileUtil.del(localPDFPath);
        }

        return resultUrl;
    }

    public String mergePDF(List<String> urlList, List<String> pdfPathList) throws IOException {
        if (pdfPathList == null) pdfPathList = new ArrayList<>();
        if (urlList == null) urlList = new ArrayList<>();

        if (pdfPathList.isEmpty() && urlList.isEmpty()) {
            throw new RuntimeException("pdf路径和url不能同时为空");
        }

        List<String> allPath = new ArrayList<>();
        if (!urlList.isEmpty()) {
            for (String url : urlList) {
                String filePath = downloadFile(url, pdfPath);
                if (!"".equals(filePath)) {
                    allPath.add(documentConvertToPDF(filePath));
                }
            }
        }

        if (!pdfPathList.isEmpty()) {
            for (String path : pdfPathList) {
                allPath.add(path);
            }
        }

        if (allPath.isEmpty()) {
            return "";
        }

        List<FileSystemResource> files = new ArrayList<>();
        for (String path : allPath) {
            files.add(new FileSystemResource(path));
        }

        BodyInserters.MultipartInserter multipartInserter = BodyInserters.fromMultipartData(
                "fileInput", files.get(0)
        );
        for (int i = 1; i < files.size(); i++) {
            multipartInserter.with("fileInput", files.get(i));
        }

        byte[] bytes = stirlingWebClient.post()
                .uri("/api/v1/general/merge-pdfs")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(multipartInserter)
                .retrieve()
                .bodyToMono(byte[].class)
                .block();

        String mergePath = pdfPath + "/merge-" + UUID.fastUUID() + ".pdf";

        FileUtil.writeBytes(bytes, mergePath);

        return mergePath;
    }

    private String downloadFile(String url, String savePath) throws IOException {
        log.info("开始处理MinIO中的Word文件: {}", url);

        String[] bucketAndPath = extractBucketAndObjectPathFromUrl(url);
        String urlBucketName = bucketAndPath[0];
        String objectPath = bucketAndPath[1];

        if (objectPath == null || objectPath.isEmpty()) {
            throw new IllegalArgumentException("无法从URL中提取对象路径: " + url);
        }

        String pdfObjectPath = getPdfObjectPath(objectPath);

        log.info("提取的存储桶名称: {}, 对象路径: {}", urlBucketName, objectPath);

        File tempWordFile = File.createTempFile("temp", "." + getFileExtension(url));
        boolean downloadSuccess = false;

        try {
            log.info("尝试使用内网下载文件...");
            downloadFileFromMinio(urlBucketName, objectPath, tempWordFile);
            downloadSuccess = true;
            log.info("内网下载成功，文件大小: {} bytes", tempWordFile.length());
        } catch (Exception e) {
            log.warn("内网下载失败: {}，尝试使用公网下载", e.getMessage());

            try {
                String publicEndpoint = minioConfig.getPublicEndpoint();
                if (publicEndpoint != null && !publicEndpoint.isEmpty()) {
                    log.info("尝试使用公网地址下载: {}", publicEndpoint);
                    downloadFileFromMinioWithEndpoint(publicEndpoint, urlBucketName, objectPath, tempWordFile);
                    downloadSuccess = true;
                    log.info("公网下载成功，文件大小: {} bytes", tempWordFile.length());
                } else {
                    throw new RuntimeException("公网地址未配置，无法下载文件");
                }
            } catch (Exception ex) {
                log.error("公网下载也失败: {}", ex.getMessage());
                throw new RuntimeException("内网和公网下载都失败", ex);
            }
        }

        if (!downloadSuccess) {
            throw new RuntimeException("无法下载Word文件");
        }

        File tempPdfFile = File.createTempFile("temp", ".pdf");
        return "";
    }

    private String documentConvertToPDF(String filePath) {
        return documentToPDF(filePath);
    }
    // 转换MinIO中的Word文档为PDF
    public String wordToPdf(String wordUrl) throws Exception {
        log.info("开始处理MinIO中的Word文件: {}", wordUrl);
        
        String[] bucketAndPath = extractBucketAndObjectPathFromUrl(wordUrl);
        String urlBucketName = bucketAndPath[0];
        String objectPath = bucketAndPath[1];
        
        if (objectPath == null || objectPath.isEmpty()) {
            throw new IllegalArgumentException("无法从URL中提取对象路径: " + wordUrl);
        }
        
        String pdfObjectPath = getPdfObjectPath(objectPath);
        
        log.info("提取的存储桶名称: {}, 对象路径: {}", urlBucketName, objectPath);
        
        File tempWordFile = File.createTempFile("temp", "." + getFileExtension(wordUrl));
        boolean downloadSuccess = false;
        
        try {
            log.info("尝试使用内网下载文件...");
            downloadFileFromMinio(urlBucketName, objectPath, tempWordFile);
            downloadSuccess = true;
            log.info("内网下载成功，文件大小: {} bytes", tempWordFile.length());
        } catch (Exception e) {
            log.warn("内网下载失败: {}，尝试使用公网下载", e.getMessage());
            
            try {
                String publicEndpoint = minioConfig.getPublicEndpoint();
                if (publicEndpoint != null && !publicEndpoint.isEmpty()) {
                    log.info("尝试使用公网地址下载: {}", publicEndpoint);
                    downloadFileFromMinioWithEndpoint(publicEndpoint, urlBucketName, objectPath, tempWordFile);
                    downloadSuccess = true;
                    log.info("公网下载成功，文件大小: {} bytes", tempWordFile.length());
                } else {
                    throw new RuntimeException("公网地址未配置，无法下载文件");
                }
            } catch (Exception ex) {
                log.error("公网下载也失败: {}", ex.getMessage());
                throw new RuntimeException("内网和公网下载都失败", ex);
            }
        }
        
        if (!downloadSuccess) {
            throw new RuntimeException("无法下载Word文件");
        }
        
        try {
            String pdfUrl = documentToPDF(tempWordFile.getAbsolutePath());
            log.info("PDF文件转换并上传成功，URL: {}", pdfUrl);
            return pdfUrl;
        } finally {
            tempWordFile.delete();
        }
    }

    // 转换本地Word文档为PDF
    public String wordToPdfLocal(String wordPath) throws Exception {
        return documentToPDF(wordPath);
    }
    // 无参构造方法，使用默认值
    public String mergePDF() throws IOException {
        return mergePDF(new ArrayList<>(), new ArrayList<>());
    }

    // 只传入urlList的重载方法
    public String mergePDF(List<String> urlList) throws IOException {
        return mergePDF(urlList, new ArrayList<>());
    }

    private void downloadFileFromMinio(String bucketName, String objectPath, File tempFile) throws Exception {
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
    private String getPdfObjectPath(String wordObjectPath) {
        int lastDotIndex = wordObjectPath.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return wordObjectPath.substring(0, lastDotIndex) + ".pdf";
        }
        return wordObjectPath + ".pdf";
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
    private String getFileExtension(String filePath) {
        int lastDotIndex = filePath.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filePath.length() - 1) {
            return filePath.substring(lastDotIndex + 1);
        }
        return "";
    }

    public List<String> pdfToImages(String pdfUrl) throws Exception {
        List<String> imageUrls = new ArrayList<>();
        
        // 下载PDF文件
        File tempPdfFile = File.createTempFile("temp", ".pdf");
        boolean downloadSuccess = false;
        
        try {
            log.info("尝试使用内网下载PDF文件...");
            String[] bucketAndPath = extractBucketAndObjectPathFromUrl(pdfUrl);
            String urlBucketName = bucketAndPath[0];
            String objectPath = bucketAndPath[1];
            
            downloadFileFromMinio(urlBucketName, objectPath, tempPdfFile);
            downloadSuccess = true;
            log.info("PDF文件下载成功，大小: {} bytes", tempPdfFile.length());
        } catch (Exception e) {
            log.warn("内网下载失败: {}，尝试使用公网下载", e.getMessage());
            
            try {
                String publicEndpoint = minioConfig.getPublicEndpoint();
                if (publicEndpoint != null && !publicEndpoint.isEmpty()) {
                    log.info("尝试使用公网地址下载: {}", publicEndpoint);
                    String[] bucketAndPath = extractBucketAndObjectPathFromUrl(pdfUrl);
                    String urlBucketName = bucketAndPath[0];
                    String objectPath = bucketAndPath[1];
                    
                    downloadFileFromMinioWithEndpoint(publicEndpoint, urlBucketName, objectPath, tempPdfFile);
                    downloadSuccess = true;
                    log.info("公网下载成功，文件大小: {} bytes", tempPdfFile.length());
                } else {
                    throw new RuntimeException("公网地址未配置，无法下载文件");
                }
            } catch (Exception ex) {
                log.error("公网下载也失败: {}", ex.getMessage());
                throw new RuntimeException("内网和公网下载都失败", ex);
            }
        }
        
        if (!downloadSuccess) {
            throw new RuntimeException("无法下载PDF文件");
        }
        
        try {
            // 调用onlyOffice API将PDF转换为图片
            FileSystemResource file = new FileSystemResource(tempPdfFile);
            
            byte[] bytes = stirlingWebClient.post()
                    .uri("/api/v1/convert/pdf/images")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData("fileInput", file))
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();
            
            if (bytes == null) {
                throw new RuntimeException("PDF转换图片失败,未获取到响应数据");
            }
            
            // 保存转换后的图片并上传到MinIO
            String tempDir = pdfPath + File.separator + UUID.randomUUID();
            FileUtil.mkdir(tempDir);
            
            // 假设返回的是zip文件，包含所有图片
            String zipPath = tempDir + File.separator + "images.zip";
            FileUtil.writeBytes(bytes, zipPath);
            
            // 解压zip文件
            cn.hutool.core.util.ZipUtil.unzip(zipPath, tempDir);
            
            // 上传图片到MinIO
            File[] imageFiles = new File(tempDir).listFiles((dir, name) -> 
                name.toLowerCase().endsWith(".png") || name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".jpeg")
            );
            
            if (imageFiles != null) {
                for (File imageFile : imageFiles) {
                    try (InputStream inputStream = FileUtil.getInputStream(imageFile)) {
                        String objectName = "images/" + UUID.randomUUID() + "." + getFileExtension(imageFile.getName());
                        minioClient.putObject(
                                PutObjectArgs.builder()
                                        .bucket(bucketName)
                                        .object(objectName)
                                        .stream(inputStream, inputStream.available(), -1)
                                        .contentType("image/png")
                                        .build()
                        );
                        
                        // 构建返回的URL
                        String publicEndpoint = minioConfig.getPublicEndpoint();
                        String imageUrl;
                        if (publicEndpoint != null && !publicEndpoint.isEmpty()) {
                            imageUrl = publicEndpoint + "/" + bucketName + "/" + objectName;
                        } else {
                            imageUrl = "http://localhost:9000/" + bucketName + "/" + objectName; // 默认地址
                        }
                        imageUrls.add(imageUrl);
                    } catch (Exception e) {
                        log.error("上传图片失败: {}", e.getMessage(), e);
                    }
                }
            }
            
            // 清理临时文件
            FileUtil.del(tempDir);
            FileUtil.del(zipPath);
            
            log.info("PDF转换图片成功，生成 {} 张图片", imageUrls.size());
        } finally {
            tempPdfFile.delete();
        }
        
        return imageUrls;
    }
}
