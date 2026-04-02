package com.jlm.homework.util;

import com.jlm.homework.entity.HomeworkStudentWriteData;
import com.jlm.homework.entity.StudentsHomeworkNew;
import com.jlm.homework.entity.StudentsWriteRecord;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 图片处理工具类
 * 处理作业图片相关的操作
 */
@Slf4j
public class HomeworkImageUtil {

    /**
     * 处理作业图片（叠加书写数据）
     * @param studentsHomework 作业对象
     * @param writeDataList 书写数据列表
     * @return 处理后的图片路径列表
     */
    public static List<String> processHomeworkImages(StudentsHomeworkNew studentsHomework, List<HomeworkStudentWriteData> writeDataList) {
        List<String> imageNames = new ArrayList<>();
        
        if (studentsHomework.getTopicImages() == null || studentsHomework.getTopicImages().isEmpty()) {
            return imageNames;
        }
        
        // 构建页码到数据的映射，提高查找效率
        Map<Integer, HomeworkStudentWriteData> pageDataMap = new HashMap<>();
        for (HomeworkStudentWriteData data : writeDataList) {
            if (data.getPageNum() != null) {
                pageDataMap.put(data.getPageNum(), data);
            }
        }
        
        // 并行处理图片，提高性能
        List<CompletableFuture<String>> futures = new ArrayList<>();
        
        for (int i = 0; i < studentsHomework.getTopicImages().size(); i++) {
            final int index = i;
            final String imageUrl = studentsHomework.getTopicImages().get(i);
            
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                if (imageUrl == null || imageUrl.isEmpty()) {
                    return null;
                }
                
                int pageNum = index + 1;
                HomeworkStudentWriteData writeData = pageDataMap.get(pageNum);
                
                if (writeData != null) {
                    try {
                        List<StudentsWriteRecord> records = writeData.getStudentsWriteRecords();
                        if (records != null && !records.isEmpty()) {
                            BufferedImage resultImage = ImageOverlayUtil.overlayWritingDataFromUrl(imageUrl, records);
                            String imageName = generateImageName(studentsHomework, pageNum);
                            CoordinateImageGenerator.saveImage(resultImage, imageName);
                            return imageName;
                        }
                    } catch (Exception e) {
                        log.warn("处理图片失败，url: {}, 错误: {}", imageUrl, e.getMessage());
                    }
                }
                return imageUrl; // 失败时使用原始URL
            });
            futures.add(future);
        }
        
        // 收集处理结果
        for (CompletableFuture<String> future : futures) {
            try {
                String result = future.get(60, TimeUnit.SECONDS);
                if (result != null) {
                    imageNames.add(result);
                }
            } catch (Exception e) {
                log.warn("获取图片处理结果失败: {}", e.getMessage());
            }
        }
        
        return imageNames;
    }
    
    /**
     * 生成图片文件名
     * @param studentsHomework 作业对象
     * @param pageNum 页码
     * @return 图片文件名
     */
    public static String generateImageName(StudentsHomeworkNew studentsHomework, int pageNum) {
        return studentsHomework.getHomeworkPublishName() + "_" + 
                studentsHomework.getStudentName() + "_" + pageNum + "页作业.png";
    }
    
    /**
     * 清理临时图片文件
     * @param imagePaths 图片路径列表
     */
    public static void cleanupTempImages(List<String> imagePaths) {
        if (imagePaths != null && !imagePaths.isEmpty()) {
            // 并行清理图片，提高性能
            imagePaths.parallelStream().forEach(imagePath -> {
                // 只删除本地文件，不删除URL
                if (!imagePath.startsWith("http://") && !imagePath.startsWith("https://")) {
                    File imageFile = new File(imagePath);
                    if (imageFile.exists() && imageFile.delete()) {
                        log.debug("删除临时图片成功: {}", imagePath);
                    }
                }
            });
        }
    }
    
    /**
     * 检查文件是否为图片
     * @param fileName 文件名
     * @return 是否为图片
     */
    public static boolean isImageFile(String fileName) {
        if (fileName == null) {
            return false;
        }
        String lowerCase = fileName.toLowerCase();
        return lowerCase.endsWith(".jpg") || lowerCase.endsWith(".jpeg") ||
               lowerCase.endsWith(".png") || lowerCase.endsWith(".gif") ||
               lowerCase.endsWith(".bmp");
    }
    
    /**
     * 检查文件是否为文档
     * @param fileName 文件名
     * @return 是否为文档
     */
    public static boolean isDocumentFile(String fileName) {
        if (fileName == null) {
            return false;
        }
        String lowerCase = fileName.toLowerCase();
        return lowerCase.endsWith(".doc") || lowerCase.endsWith(".docx") || lowerCase.endsWith(".pdf");
    }
    
    /**
     * 检查文件是否为PDF文件
     * @param fileName 文件名
     * @return 是否为PDF文件
     */
    public static boolean isPdfFile(String fileName) {
        if (fileName == null) {
            return false;
        }
        String lowerCase = fileName.toLowerCase();
        return lowerCase.endsWith(".pdf");
    }
    
    /**
     * 从URL获取文件名
     * @param url URL地址
     * @return 文件名
     */
    public static String getFileNameFromUrl(String url) {
        if (url == null) {
            return null;
        }
        int lastSlashIndex = url.lastIndexOf('/');
        if (lastSlashIndex != -1 && lastSlashIndex < url.length() - 1) {
            return url.substring(lastSlashIndex + 1);
        }
        return url;
    }
}