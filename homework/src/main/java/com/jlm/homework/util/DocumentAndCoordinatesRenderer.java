package com.jlm.homework.util;

import com.jlm.homework.entity.HomeworkStudentWriteData;
import com.jlm.homework.entity.StudentsWriteRecord;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * 文档和坐标点渲染工具类
 * 实现从URL下载.docx文档、转换为图片并叠加StudentsWriteRecord坐标点
 */
public class DocumentAndCoordinatesRenderer {
    

    
    /**
     * 从URL下载文档并保存到临时文件
     * 
     * @param documentUrl 文档URL
     * @return 临时文件对象
     * @throws IOException 下载异常
     */
    public static File downloadDocument(String documentUrl) throws IOException {
        File tempFile = File.createTempFile("temp_doc", ".docx");
        tempFile.deleteOnExit(); // JVM退出时自动删除
        
        try (InputStream in = new URL(documentUrl).openStream();
             OutputStream out = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
        
        return tempFile;
    }
    
    /**
     * 将DOCX文档转换为图片
     * 注意：这是一个简化实现，实际应用可能需要更复杂的渲染逻辑
     * 
     * @param docxFile DOCX文件
     * @param pageNum 页码（从1开始）
     * @return 生成的图片
     * @throws IOException IO异常
     * @throws InvalidFormatException 格式异常
     */
    public static BufferedImage convertDocxToImage(File docxFile, int pageNum) 
            throws IOException, InvalidFormatException {
        // 创建一个简单的图片作为文档页面的表示
        // 实际应用中可以使用更专业的文档渲染库
        BufferedImage image = new BufferedImage(800, 1100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        
        // 填充白色背景
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, 800, 1100);
        
        // 添加文档内容（简化实现）
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("SimSun", Font.PLAIN, 12));
        
        // 尝试读取文档内容
        try (XWPFDocument document = new XWPFDocument(new FileInputStream(docxFile))) {
            XWPFParagraph paragraph = document.getParagraphArray(Math.min(pageNum - 1, document.getParagraphs().size() - 1));
            if (paragraph != null) {
                String text = paragraph.getText();
                // 简单绘制文本内容
                g2d.drawString("页面 " + pageNum + ": " + text, 50, 50);
            }
        }
        
        g2d.dispose();
        return image;
    }
    
    /**
     * 在图片上绘制坐标点
     * 
     * @param image 原始图片
     * @param coordinates 坐标点列表
     * @param targetPageNum 目标页码
     * @param color 绘制颜色
     * @param pointSize 点的大小
     * @return 绘制后的图片
     */
    public static BufferedImage drawCoordinates(BufferedImage image, List<HomeworkStudentWriteData> coordinates,
                                             int targetPageNum, Color color, int pointSize) {
        // 创建图片副本
        BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = result.createGraphics();
        
        // 绘制原始图片
        g2d.drawImage(image, 0, 0, null);
        
        // 设置绘制参数
        g2d.setColor(color);
        
        // 绘制坐标点
        for (HomeworkStudentWriteData coord : coordinates) {
            if (coord.getPageNum() == targetPageNum) {
                for (StudentsWriteRecord record:coord.getStudentsWriteRecords()) {
                    // 调整坐标点大小
                    g2d.fillOval(record.getX() - pointSize / 2, record.getY() - pointSize / 2,
                            pointSize, pointSize);
                }
            }
        }
        
        g2d.dispose();
        return result;
    }
    
    /**
     * 生成包含文档和坐标点的完整图片
     * 
     * @param documentUrl 文档URL
     * @param coordinates 坐标点列表
     * @param pageNum 页码
     * @param outputPath 输出路径
     * @throws IOException IO异常
     * @throws InvalidFormatException 格式异常
     */
    public static void generateDocumentWithCoordinates(String documentUrl, List<HomeworkStudentWriteData> coordinates,
                                                    int pageNum, String outputPath) 
            throws IOException, InvalidFormatException {
        // 1. 下载文档
        File docxFile = downloadDocument(documentUrl);
        
        try {
            // 2. 转换文档为图片
            BufferedImage docImage = convertDocxToImage(docxFile, pageNum);
            
            // 3. 在图片上绘制坐标点
            BufferedImage resultImage = drawCoordinates(docImage, coordinates, pageNum,
                                                      Color.RED, 3); // 红色点，大小为3
            
            // 4. 保存结果图片
            File outputFile = new File(outputPath);
            ImageIO.write(resultImage, "PNG", outputFile);
            
            System.out.println("图片生成成功: " + outputPath);
        } finally {
            // 清理临时文件
            docxFile.delete();
        }
    }
    
    /**
     * 测试方法
     */
    public static void main(String[] args) {
        try {
            // 示例文档URL
            String documentUrl = "http://127.0.0.1:9930/bucket/download/d5fb8e5699ea4d7b938affd2368e1114/211/off/四年级数学9月26日作业（09月26日）.docx";
            
            // 创建示例坐标点数据（实际应用中应从StudentsWriteRecord获取）
            List<HomeworkStudentWriteData> coordinates = new ArrayList<>();

            
            // 生成图片
            String outputPath = "output_document_with_coordinates.png";
            generateDocumentWithCoordinates(documentUrl, coordinates, 1, outputPath);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}