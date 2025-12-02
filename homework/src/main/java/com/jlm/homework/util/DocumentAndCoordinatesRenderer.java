package com.jlm.homework.util;

import com.jlm.homework.entity.HomeworkStudentWriteData;
import com.jlm.homework.entity.StudentsWriteRecord;
import com.alibaba.cloud.commons.lang.StringUtils;
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
        if(StringUtils.isEmpty(documentUrl)){
            throw new IOException("文档URL不能为空！");
        }
        
        // 处理URL编码问题，特别是文件名中的中文和特殊字符
        try {
            // 解析URL，分别编码路径部分
            URL originalUrl = new URL(documentUrl);
            String protocol = originalUrl.getProtocol();
            String host = originalUrl.getHost();
            int port = originalUrl.getPort();
            String path = originalUrl.getPath();
            String query = originalUrl.getQuery();
            String ref = originalUrl.getRef();
            
            // 对路径进行编码处理，确保中文和特殊字符正确编码
            String encodedPath = encodeUrlPath(path);
            
            // 重新构建URL
            StringBuilder urlBuilder = new StringBuilder();
            urlBuilder.append(protocol).append("://").append(host);
            if (port != -1) {
                urlBuilder.append(":").append(port);
            }
            urlBuilder.append(encodedPath);
            if (query != null) {
                urlBuilder.append("?").append(query);
            }
            if (ref != null) {
                urlBuilder.append("#").append(ref);
            }
            
            URL encodedUrl = new URL(urlBuilder.toString());
            
            // 创建URL连接并设置请求属性
            java.net.URLConnection connection = encodedUrl.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setConnectTimeout(30000); // 30秒连接超时
            connection.setReadTimeout(60000); // 60秒读取超时
            connection.setDoInput(true);
            
            // 检查Content-Type
            String contentType = connection.getContentType();
            System.out.println("下载文档Content-Type: " + contentType);
            
            // 验证Content-Type是否为预期的文档类型
            if (!isValidContentType(contentType)) {
                // 即使Content-Type不符合预期，也尝试下载并检查文件内容
                System.out.println("警告：Content-Type不符合预期，但将继续下载并验证文件内容");
            }
            
            // 创建临时文件
            File tempFile = File.createTempFile("temp_doc", ".docx");
            tempFile.deleteOnExit(); // JVM退出时自动删除
            
            // 下载文件内容
            try (InputStream in = connection.getInputStream();
                 OutputStream out = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                int totalBytesRead = 0;
                
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                }
                
                // 检查是否有内容被读取
                if (totalBytesRead == 0) {
                    tempFile.delete();
                    throw new IOException("下载的文档为空，无法处理。URL: " + documentUrl);
                }
                
                System.out.println("成功下载文档，文件大小: " + totalBytesRead + " 字节");
            }
            
            // 下载后进行文件内容验证
            if (!validateFileContent(tempFile)) {
                tempFile.delete(); // 删除无效文件
                throw new IOException("下载的文档内容无效，文件格式不正确或已损坏。URL: " + documentUrl);
            }
            
            return tempFile;
        } catch (IOException e) {
            // 增强错误信息，包含详细的URL和错误原因
            throw new IOException("下载文档失败: " + documentUrl + "，错误: " + e.getMessage(), e);
        }
    }
    
    /**
     * 验证Content-Type是否为有效的文档类型
     * 
     * @param contentType HTTP响应的Content-Type
     * @return 是否为有效的文档类型
     */
    private static boolean isValidContentType(String contentType) {
        if (contentType == null) {
            return false;
        }
        
        // 支持的文档Content-Type列表
        String[] validContentTypes = {
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // .docx
            "application/msword", // .doc
            "application/zip", // 有时docx文件可能被识别为zip
            "application/octet-stream" // 二进制流，通用情况
        };
        
        for (String validType : validContentTypes) {
            if (contentType.startsWith(validType)) {
                return true;
            }
        }
        
        // 检查是否包含word或document关键词
        return contentType.toLowerCase().contains("word") || 
               contentType.toLowerCase().contains("document") ||
               contentType.toLowerCase().contains("docx") ||
               contentType.toLowerCase().contains("doc");
    }
    
    /**
     * 验证文件内容是否有效
     * 
     * @param file 要验证的文件
     * @return 文件内容是否有效
     */
    private static boolean validateFileContent(File file) {
        try {
            // 检查文件是否为空
            if (file.length() == 0) {
                System.err.println("验证失败：文件为空");
                return false;
            }
            
            // 检查文件是否为有效的ZIP格式（.docx本质上是ZIP文件）
            try (FileInputStream fis = new FileInputStream(file)) {
                // 检查ZIP文件的魔术数字（前两个字节应该是PK）
                byte[] header = new byte[2];
                int bytesRead = fis.read(header);
                if (bytesRead == 2 && header[0] == 'P' && header[1] == 'K') {
                    System.out.println("文件验证通过：是有效的ZIP格式");
                    return true;
                }
            }
            
            // 如果不是ZIP格式，尝试检查是否为旧版.doc格式（OLE2格式）
            // 旧版.doc文件通常以D0 CF 11 E0 A1 B1 1A E1开头（OLE2魔术数字）
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] header = new byte[8];
                int bytesRead = fis.read(header);
                if (bytesRead >= 8) {
                    boolean isOle2Format = (header[0] == (byte)0xD0 && 
                                          header[1] == (byte)0xCF && 
                                          header[2] == (byte)0x11 && 
                                          header[3] == (byte)0xE0 && 
                                          header[4] == (byte)0xA1 && 
                                          header[5] == (byte)0xB1 && 
                                          header[6] == (byte)0x1A && 
                                          header[7] == (byte)0xE1);
                    
                    if (isOle2Format) {
                        System.out.println("文件验证通过：是有效的OLE2格式（可能是.doc文件）");
                        return true;
                    }
                }
            }
            
            // 如果文件格式不匹配预期，记录详细信息
            System.err.println("验证失败：文件格式不是有效的.docx或.doc格式");
            System.err.println("文件大小：" + file.length() + " 字节");
            
            // 检查文件是否为HTML或错误页面
            try (FileInputStream fis = new FileInputStream(file);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(fis, "UTF-8"))) {
                String firstLine = reader.readLine();
                if (firstLine != null && firstLine.toLowerCase().contains("<!doctype html") || 
                    firstLine != null && firstLine.toLowerCase().contains("<html")) {
                    System.err.println("警告：下载的可能是HTML页面而不是文档文件");
                }
            } catch (Exception e) {
                // 忽略字符编码错误
            }
            
            return false;
        } catch (Exception e) {
            System.err.println("文件验证过程中发生错误: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 对URL路径进行编码，保留路径分隔符但编码其他特殊字符
     * 
     * @param path 原始路径
     * @return 编码后的路径
     */
    private static String encodeUrlPath(String path) {
        if (StringUtils.isEmpty(path)) {
            return path;
        }
        
        StringBuilder encodedPath = new StringBuilder();
        String[] pathSegments = path.split("/");
        
        for (int i = 0; i < pathSegments.length; i++) {
            if (i > 0) {
                encodedPath.append("/");
            }
            if (!pathSegments[i].isEmpty()) {
                try {
                    // 对每个路径段单独进行URL编码
                    encodedPath.append(java.net.URLEncoder.encode(pathSegments[i], "UTF-8")
                            .replace("+", "%20")
                            .replace("%21", "!")
                            .replace("%27", "'")
                            .replace("%28", "(")
                            .replace("%29", ")")
                            .replace("%7E", "~")
                    );
                } catch (UnsupportedEncodingException e) {
                    // UTF-8编码支持是标准的，不太可能抛出此异常
                    encodedPath.append(pathSegments[i]);
                }
            }
        }
        
        // 保持原始路径的末尾斜杠
        if (path.endsWith("/")) {
            encodedPath.append("/");
        }
        
        return encodedPath.toString();
    }
    
    /**
     * 将文档转换为图片，主要支持.docx格式
     * 注意：这是一个简化实现，实际应用可能需要更复杂的渲染逻辑
     * 
     * @param docFile 文档文件
     * @param pageNum 页码（从1开始）
     * @return 生成的图片
     * @throws IOException IO异常
     */
    public static BufferedImage convertDocxToImage(File docFile, int pageNum) throws IOException {
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
        
        // 检测文件扩展名和基本验证
        String fileName = docFile.getName().toLowerCase();
        String textContent = "";
        
        // 检查文件是否存在且非空
        if (!docFile.exists() || docFile.length() == 0) {
            textContent = "文档文件不存在或为空文件。";
        } else {
            try {
                // 首先检查文件扩展名
                if (fileName.endsWith(".doc") || fileName.endsWith(".dot")) {
                    textContent = "检测到.doc格式文档，当前版本暂不支持直接解析。请使用.docx格式或考虑添加POI HWPF依赖。";
                } else if (fileName.endsWith(".docx") || fileName.endsWith(".dotx")) {
                    // 处理OOXML格式（.docx）
                    try (FileInputStream fis = new FileInputStream(docFile)) {
                        // 在尝试创建XWPFDocument之前，先进行简单的ZIP格式检查
                        if (!isValidZipFile(fis)) {
                            textContent = "文档格式错误：.docx文件应该是有效的ZIP格式，但检测到格式异常。文件可能已损坏或不是真正的.docx文件。";
                        } else {
                            // 重置流位置
                            fis.getChannel().position(0);
                            try (XWPFDocument document = new XWPFDocument(fis)) {
                                if (document.getParagraphs() != null && !document.getParagraphs().isEmpty()) {
                                    XWPFParagraph paragraph = document.getParagraphArray(Math.min(pageNum - 1, document.getParagraphs().size() - 1));
                                    if (paragraph != null) {
                                        textContent = paragraph.getText();
                                    } else {
                                        textContent = "页面内容为空或页码超出范围。";
                                    }
                                } else {
                                    textContent = "文档中没有发现段落内容。";
                                }
                            }
                        }
                    } catch (org.apache.poi.openxml4j.exceptions.OLE2NotOfficeXmlFileException e) {
                        // 特别处理OLE2格式异常
                        textContent = "文档格式错误：文件虽然扩展名是.docx，但实际为旧版OLE2格式。请重新保存为正确的.docx格式。";
                        System.err.println("OLE2格式错误 - 文件名: " + fileName + ", 错误: " + e.getMessage());
                    } catch (Exception e) {
                        // 针对ZipArchiveThresholdInputStream相关错误的专门处理
                        String errorMsg = e.getMessage();
                        if (errorMsg != null && (errorMsg.contains("No valid entries or contents found") || 
                                                errorMsg.contains("not a valid OOXML") || 
                                                errorMsg.contains("ZipArchiveThresholdInputStream"))) {
                            textContent = "文档格式无效：检测到无效的OOXML文件。文件可能已损坏、不完整或格式错误。请确保上传的是标准的.docx文档。";
                        } else {
                            textContent = "处理文档时出错：" + (errorMsg != null ? errorMsg : "未知错误");
                        }
                        System.err.println("OOXML解析错误 - 文件名: " + fileName + ", 错误: " + e.getMessage());
                    }
                } else {
                    // 对于其他非.docx格式，提供友好提示
                    textContent = "不支持的文档格式: " + fileName + "，仅支持.docx格式。";
                }
            } catch (Exception e) {
                // 捕获所有其他异常，确保方法不会因文档格式问题而完全失败
                String errorMsg = e.getMessage();
                textContent = "无法处理文档: " + (errorMsg != null ? errorMsg : "未知错误");
                System.err.println("文档处理错误 - 文件名: " + fileName + ", 错误: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        // 绘制文本内容，添加格式信息
        g2d.drawString("页面 " + pageNum + " (" + fileName + "):", 50, 50);
        
        // 绘制文档内容（限制文本长度，避免绘制问题）
        if (textContent.length() > 100) {
            textContent = textContent.substring(0, 100) + "...";
        }
        g2d.drawString(textContent, 50, 70);
        
        // 如果内容是错误信息，添加额外的提示
        if (textContent.contains("错误") || textContent.contains("不支持") || textContent.contains("无法") || 
            textContent.contains("无效")) {
            g2d.setColor(Color.RED);
            g2d.drawString("提示：请确认文档格式正确并使用有效的.docx扩展名。", 50, 90);
            g2d.drawString("如需支持其他格式，请联系技术支持。", 50, 110);
        }
        
        g2d.dispose();
        return image;
    }
    
    /**
     * 简单检查文件是否为有效的ZIP格式（.docx本质上是ZIP文件）
     * 
     * @param fis 文件输入流
     * @return 是否为有效的ZIP格式
     */
    private static boolean isValidZipFile(FileInputStream fis) {
        try {
            // 检查ZIP文件的魔术数字（前两个字节应该是PK）
            byte[] header = new byte[2];
            int bytesRead = fis.read(header);
            if (bytesRead == 2) {
                return header[0] == 'P' && header[1] == 'K';
            }
            return false;
        } catch (IOException e) {
            return false;
        }
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