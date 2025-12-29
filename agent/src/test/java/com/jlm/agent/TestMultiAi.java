package com.jlm.agent;


//import com.jlm.agent.AIServ.TongYiServ;

import com.jlm.agent.AIServ.ZhiPuServ;
import jakarta.annotation.Resource;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.ai.content.Media;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


@SpringBootTest
public class TestMultiAi {

    @Resource
    ZhiPuAiChatModel zhiPuAiChatModel;
    @Resource
    ZhiPuServ zhiPuServ;



    /**
     *  调用通义千问 统一图文模型，进行图文解析
     */
//    @Test
//    void testTongyiSDK() {
//
//        List<String> imageUrls = new ArrayList<>();
//
//        // 一 内网图片，转base64发送，注意拼接前缀 data:image/.
//        String localPath = "http://192.168.1.29/homework-design/assets/background-c4d9e324.jpg";
//        String base64Pic = null;
//        try {
//            base64Pic = downloadImageToBase64(localPath);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//        imageUrls.add(base64Pic);
//
//        // 二 外网图片，直接传图片地址
//        imageUrls.add("https://img1.baidu.com/it/u=2172818577,3783888802&fm=253&app=138&f=JPEG?w=800&h=1422");
//
//        String ret = tongYiServ.multiModalCall("描述所有图片内容", imageUrls);
//        //System.out.println(ret);
//    }



    @Test
    void test() {
        String string = zhiPuAiChatModel.call("你好");
        //System.out.println(string);
    }

    @Test
    void test2() {
        String picUrl = "https://tongyi-main.oss-accelerate.aliyuncs.com/upload/20251208/93de276e61184400affdb130b1d1784e/31bec0e8f1284b2fbe9bcecc2b0d826c/%E8%AF%95%E9%A2%981-%E7%BB%86%E7%AC%94%E7%94%BB-%E6%BD%A6%E8%8D%89.png?Expires=1828235950&OSSAccessKeyId=LTAI5tL97mBYzVcjkG1cUyin&Signature=8htWxZxbopfZNNTMKa2VP7nkhDU%3D";
        List<Media> medias = new ArrayList<Media>();
        Media media = new Media(MediaType.IMAGE_PNG, URI.create(picUrl));
        medias.add(media);
        String string = zhiPuServ.multiModalCall("请描述图片内容", medias);
        //System.out.println(string);
    }



    private static byte[] downloadImageByUrl(String imageUrl) throws Exception {
        try (var inputStream = new URL(imageUrl).openStream()) {
            return IOUtils.toByteArray(inputStream);
        }
    }

    /**
     * 将图片转为 Base64 字符串（含 data:image/... 前缀）
     */
    private static String downloadImageToBase64(String imageUrl) throws Exception {
        byte[] imageBytes = downloadImageByUrl(imageUrl);
        String mimeType = guessMimeType(imageUrl);
        return "data:" + mimeType + ";base64," + java.util.Base64.getEncoder().encodeToString(imageBytes);
    }

    private static String guessMimeType(String url) {
        String lower = url.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        return "image/jpeg"; // 默认
    }


   /* @Test
    void testZhiPuAI() {
        //System.out.println(zhiPuAIServ.doChatWithReport("提取王二的答题内容，结构化输出", "https://img0.baidu.com/it/u=2412944529,4254979042&fm=253&app=138&f=JPEG?w=800&h=1067"));
    }*/


}
