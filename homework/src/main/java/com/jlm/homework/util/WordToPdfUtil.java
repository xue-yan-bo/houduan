package com.jlm.homework.util;

import com.jlm.homework.config.MinioConfig;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.docx4j.Docx4J;
import org.docx4j.fonts.IdentityPlusMapper;
import org.docx4j.fonts.Mapper;
import org.docx4j.fonts.PhysicalFont;
import org.docx4j.fonts.PhysicalFonts;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.concurrent.TimeUnit;

/**
 * Word文档转PDF工具类 - 使用多种策略确保转换质量和稳定性
 * 策略优先级：1. LibreOffice(最佳质量) 2. docx4j(纯Java方案)
 */
@Slf4j
@Component
public class WordToPdfUtil {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;
    private Mapper fontMapper;
    private boolean libreOfficeAvailable = false;

    @Autowired
    public WordToPdfUtil(MinioClient minioClient, MinioConfig minioConfig) {
        this.minioClient = minioClient;
        this.minioConfig = minioConfig;
        initFontMapper();
        checkLibreOfficeAvailability();
        log.info("WordToPdfUtil初始化成功");
        log.info("LibreOffice可用: {}", libreOfficeAvailable);
        log.info("MinIO内网endpoint: {}, 公网endpoint: {}", minioConfig.getEndpoint(), minioConfig.getPublicEndpoint());
    }

    /**
     * 检查LibreOffice是否可用
     */
    private void checkLibreOfficeAvailability() {
        try {
            ProcessBuilder pb = new ProcessBuilder("soffice", "--version");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (finished && process.exitValue() == 0) {
                libreOfficeAvailable = true;
                log.info("LibreOffice已安装并可用");
            } else {
                log.warn("LibreOffice检查失败，将使用docx4j作为备选方案");
            }
        } catch (Exception e) {
            log.warn("LibreOffice未安装或不可用: {}，将使用docx4j作为备选方案", e.getMessage());
        }
    }

    /**
     * 初始化字体映射器 - 解决中文乱码问题
     */
    private void initFontMapper() {
        try {
            fontMapper = new IdentityPlusMapper();
            
            String os = System.getProperty("os.name").toLowerCase();
            log.info("当前操作系统: {}", os);
            
            // 先发现系统字体
            PhysicalFonts.discoverPhysicalFonts();
            java.util.Map<String, PhysicalFont> physicalFonts = PhysicalFonts.getPhysicalFonts();
            log.info("系统字体扫描完成，发现 {} 种字体", physicalFonts.size());
            
            // 打印所有可用的字体
            if (log.isDebugEnabled()) {
                for (String fontName : physicalFonts.keySet()) {
                    log.debug("可用字体: {}", fontName);
                }
            }
            
            // 映射字体
            mapFonts(fontMapper, physicalFonts);
            
            log.info("字体映射器初始化完成");
            
        } catch (Exception e) {
            log.warn("字体映射器初始化失败，使用默认配置: {}", e.getMessage());
            fontMapper = new IdentityPlusMapper();
        }
    }

    /**
     * 映射字体
     */
    private void mapFonts(Mapper mapper, java.util.Map<String, PhysicalFont> physicalFonts) {
        // 1. 先查找中文字体
        PhysicalFont chineseFont = findChineseFont(physicalFonts);
        
        // 2. 查找英文字体和数学符号字体
        PhysicalFont englishFont = findEnglishFont(physicalFonts);
        PhysicalFont mathFont = findMathFont(physicalFonts);
        
        // 3. 映射常见字体
        mapCommonFonts(mapper, chineseFont, englishFont, mathFont);
        
        // 4. 确保有默认字体
        setupDefaultFonts(mapper, chineseFont, englishFont, mathFont);
    }

    /**
     * 查找中文字体
     */
    private PhysicalFont findChineseFont(java.util.Map<String, PhysicalFont> physicalFonts) {
        String[] chineseFontNames = {"SimSun", "宋体", "SimHei", "黑体", "Microsoft YaHei", "微软雅黑", 
                                  "KaiTi", "楷体", "FangSong", "仿宋"};
        
        for (String fontName : chineseFontNames) {
            PhysicalFont font = physicalFonts.get(fontName);
            if (font != null) {
                log.info("找到中文字体: {} -> {}", fontName, font.getName());
                return font;
            }
        }
        
        // 尝试查找包含中文字符的字体
        for (java.util.Map.Entry<String, PhysicalFont> entry : physicalFonts.entrySet()) {
            String fontName = entry.getKey().toLowerCase();
            if (fontName.contains("song") || fontName.contains("hei") || fontName.contains("yahei") || 
                fontName.contains("zen") || fontName.contains("ping") || fontName.contains("kai")) {
                log.info("找到备选中文字体: {} -> {}", entry.getKey(), entry.getValue().getName());
                return entry.getValue();
            }
        }
        
        log.warn("未找到中文字体");
        return null;
    }

    /**
     * 查找英文字体
     */
    private PhysicalFont findEnglishFont(java.util.Map<String, PhysicalFont> physicalFonts) {
        String[] englishFontNames = {"Times New Roman", "Arial", "Calibri", "Verdana", "Courier New"};
        
        for (String fontName : englishFontNames) {
            PhysicalFont font = physicalFonts.get(fontName);
            if (font != null) {
                log.info("找到英文字体: {} -> {}", fontName, font.getName());
                return font;
            }
        }
        
        log.warn("未找到英文字体");
        return null;
    }

    /**
     * 查找数学符号字体
     */
    private PhysicalFont findMathFont(java.util.Map<String, PhysicalFont> physicalFonts) {
        String[] mathFontNames = {"Cambria Math", "Symbol", "Times New Roman", "Arial"};
        
        for (String fontName : mathFontNames) {
            PhysicalFont font = physicalFonts.get(fontName);
            if (font != null) {
                log.info("找到数学符号字体: {} -> {}", fontName, font.getName());
                return font;
            }
        }
        
        log.warn("未找到数学符号字体");
        return null;
    }

    /**
     * 映射常见字体
     */
    private void mapCommonFonts(Mapper mapper, PhysicalFont chineseFont, PhysicalFont englishFont, PhysicalFont mathFont) {
        // 中文字体映射
        if (chineseFont != null) {
            String[] chineseFonts = {"宋体", "SimSun", "黑体", "SimHei", "微软雅黑", "Microsoft YaHei", 
                                  "楷体", "KaiTi", "仿宋", "FangSong"};
            for (String fontName : chineseFonts) {
                mapper.put(fontName, chineseFont);
            }
        }
        
        // 英文字体映射
        if (englishFont != null) {
            String[] englishFonts = {"Times New Roman", "Arial", "Calibri", "Verdana", "Courier New"};
            for (String fontName : englishFonts) {
                mapper.put(fontName, englishFont);
            }
        }
        
        // 数学符号字体映射
        if (mathFont != null) {
            String[] mathFonts = {"Cambria Math", "Symbol", "Math", "Mathematical"};
            for (String fontName : mathFonts) {
                mapper.put(fontName, mathFont);
            }
        }
    }

    /**
     * 设置默认字体
     */
    private void setupDefaultFonts(Mapper mapper, PhysicalFont chineseFont, PhysicalFont englishFont, PhysicalFont mathFont) {
        // 确保至少有一个默认字体
        PhysicalFont defaultFont = null;
        if (chineseFont != null) defaultFont = chineseFont;
        else if (englishFont != null) defaultFont = englishFont;
        else if (mathFont != null) defaultFont = mathFont;
        else if (!PhysicalFonts.getPhysicalFonts().isEmpty()) {
            defaultFont = PhysicalFonts.getPhysicalFonts().entrySet().iterator().next().getValue();
            log.info("使用第一个可用字体作为默认: {}", defaultFont.getName());
        }
        
        if (defaultFont != null) {
            // 映射通用字体名称
            String[] defaultFonts = {"serif", "sans-serif", "monospace", "default", "normal"};
            for (String fontName : defaultFonts) {
                mapper.put(fontName, defaultFont);
            }
            
            // 确保常用字体有映射
            if (chineseFont == null) {
                String[] chineseFonts = {"宋体", "SimSun", "黑体", "SimHei"};
                for (String fontName : chineseFonts) {
                    mapper.put(fontName, defaultFont);
                }
            }
            
            if (englishFont == null) {
                String[] englishFonts = {"Times New Roman", "Arial"};
                for (String fontName : englishFonts) {
                    mapper.put(fontName, defaultFont);
                }
            }
            
            if (mathFont == null) {
                String[] mathFonts = {"Cambria Math", "Symbol"};
                for (String fontName : mathFonts) {
                    mapper.put(fontName, defaultFont);
                }
            }
        }
    }

    /**
     * 将Word文档转换为PDF - 使用最佳可用策略
     */
    public void convertWordToPdf(String inputPath, String outputPath) throws Exception {
        if (inputPath == null || outputPath == null) {
            throw new IllegalArgumentException("输入路径和输出路径不能为空");
        }

        File inputFile = new File(inputPath);
        if (!inputFile.exists()) {
            throw new FileNotFoundException("Word文档不存在: " + inputPath);
        }

        // 优先使用LibreOffice，如果不可用则使用docx4j
        if (libreOfficeAvailable) {
            try {
                convertWithLibreOffice(inputPath, outputPath);
                return;
            } catch (Exception e) {
                log.warn("LibreOffice转换失败，尝试使用docx4j: {}", e.getMessage());
            }
        }
        
        // 使用docx4j作为备选
        convertWithDocx4j(inputPath, outputPath);
    }

    /**
     * 使用LibreOffice转换Word到PDF - 最佳质量
     */
    private void convertWithLibreOffice(String inputPath, String outputPath) throws Exception {
        log.info("使用LibreOffice转换Word到PDF: {} -> {}", inputPath, outputPath);
        
        File inputFile = new File(inputPath);
        File outputFile = new File(outputPath);
        File outputDir = outputFile.getParentFile();
        
        // LibreOffice命令
        ProcessBuilder pb = new ProcessBuilder(
            "soffice",
            "--headless",
            "--convert-to", "pdf",
            "--outdir", outputDir.getAbsolutePath(),
            inputFile.getAbsolutePath()
        );
        
        pb.redirectErrorStream(true);
        Process process = pb.start();
        
        // 读取输出
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        
        // 等待进程完成
        boolean finished = process.waitFor(60, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new Exception("LibreOffice转换超时");
        }
        
        if (process.exitValue() != 0) {
            throw new Exception("LibreOffice转换失败: " + output.toString());
        }
        
        // LibreOffice生成的PDF文件名可能与预期不同，需要重命名
        String expectedPdfName = inputFile.getName().replaceAll("\\.[^.]+$", ".pdf");
        File generatedPdf = new File(outputDir, expectedPdfName);
        if (generatedPdf.exists() && !generatedPdf.getAbsolutePath().equals(outputPath)) {
            if (!generatedPdf.renameTo(outputFile)) {
                // 如果重命名失败，复制文件
                try (InputStream is = new FileInputStream(generatedPdf);
                     OutputStream os = new FileOutputStream(outputFile)) {
                    byte[] buffer = new byte[1024 * 1024];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }
                }
                generatedPdf.delete();
            }
        }
        
        if (!outputFile.exists()) {
            throw new Exception("LibreOffice未能生成PDF文件");
        }
        
        log.info("LibreOffice转换完成: {}", outputPath);
    }

    /**
     * 使用docx4j转换Word到PDF - 纯Java方案
     */
    private void convertWithDocx4j(String inputPath, String outputPath) throws Exception {
        log.info("使用docx4j转换Word到PDF: {} -> {}", inputPath, outputPath);
        
        WordprocessingMLPackage wordMLPackage = null;
        try {
            wordMLPackage = WordprocessingMLPackage.load(new File(inputPath));
            
            // 设置字体映射器
            wordMLPackage.setFontMapper(fontMapper);
            
            // 确保所有内容都被处理
            MainDocumentPart mainDocumentPart = wordMLPackage.getMainDocumentPart();
            if (mainDocumentPart != null) {
                log.info("文档主部分加载成功，包含内容: {}", 
                    mainDocumentPart.getContent() != null ? mainDocumentPart.getContent().size() + " 个元素" : "空");
            }
            
            // 转换为PDF
            try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                Docx4J.toPDF(wordMLPackage, fos);
            }
            
            log.info("docx4j转换完成: {}", outputPath);
            
        } catch (Exception e) {
            log.error("docx4j转换失败: {}", e.getMessage(), e);
            throw new Exception("Word转PDF失败: " + e.getMessage(), e);
        }
    }

    /**
     * 转换MinIO中的Word文档为PDF
     */
    public String convertMinioWordToPdf(String wordUrl) throws Exception {
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
        
        File tempPdfFile = File.createTempFile("temp", ".pdf");
        try {
            convertWordToPdf(tempWordFile.getAbsolutePath(), tempPdfFile.getAbsolutePath());
            
            log.info("PDF转换完成，文件大小: {} bytes", tempPdfFile.length());
            
            try (FileInputStream fis = new FileInputStream(tempPdfFile)) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(urlBucketName)
                                .object(pdfObjectPath)
                                .stream(fis, tempPdfFile.length(), -1)
                                .build()
                );
            }
            
            String pdfUrl = wordUrl.replace(getFileExtension(wordUrl), "pdf");
            log.info("PDF文件转换并上传成功，URL: {}", pdfUrl);
            return pdfUrl;
        } finally {
            tempWordFile.delete();
            tempPdfFile.delete();
        }
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

    private String getPdfObjectPath(String wordObjectPath) {
        int lastDotIndex = wordObjectPath.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return wordObjectPath.substring(0, lastDotIndex) + ".pdf";
        }
        return wordObjectPath + ".pdf";
    }

    private String getFileExtension(String filePath) {
        int lastDotIndex = filePath.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filePath.length() - 1) {
            return filePath.substring(lastDotIndex + 1);
        }
        return "";
    }
}
