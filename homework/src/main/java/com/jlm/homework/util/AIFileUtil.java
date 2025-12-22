package com.jlm.homework.util;

import org.springframework.util.Base64Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class AIFileUtil {

    /**
     * 将图片编码为Base64字符串
     *
     * @param imagePath 图片路径（支持本地文件路径或HTTP URL）
     * @return Base64编码后的图片数据
     * @throws IOException 读取异常
     */
    public static String encodeImageToBase64(String imagePath) throws IOException {
        // 规范化URL格式，将反斜杠替换为正斜杠，确保http://格式正确
        String normalizedPath = imagePath;
        if (!normalizedPath.startsWith("http://") && !normalizedPath.startsWith("https://")) {
            normalizedPath = imagePath.replace("\\", "/")
                    .replace("http:/", "http://");
        }
        if (normalizedPath.startsWith("http://") || normalizedPath.startsWith("https://")) {
            // 处理网络图片
            URL url = new URL(normalizedPath);
            try (InputStream is = url.openStream()) {
                byte[] bytes = is.readAllBytes();
                return Base64Utils.encodeToString(bytes);
            }
        } else {
            // 处理本地文件
            File file = new File(normalizedPath);
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] bytes = new byte[(int) file.length()];
                fis.read(bytes);
                return Base64Utils.encodeToString(bytes);
            }
        }
    }
}
