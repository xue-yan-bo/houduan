package com.jlm.homework.util;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author QingYang
 * @version 1.0
 * @description
 * @date 2023/2/21 0021
 */
@Slf4j
@Component
public class BasicUtil {

    public static final String CHINES_NUMBER = "一十";

    private static final String DOWNLOAD_MINIO = "http://124.165.206.34:20029";

    /**
     * 获取用户完成token信息
     *
     * @param prefixToken token前缀
     */
    /*public static String buildRedisKeyByToken(String prefixToken) {
        final HttpServletRequest request = ServletUtils.getRequest();
        if (ObjectUtil.isEmpty(request)) {
            throw new JlmCustomException(JlmExceptionEnum.REQUEST_PARAM_ERROR.getResultCode(), "未授权的请求，请重新登录.");
        }
        final String token = SecurityUtils.getToken(request);
        if (StrUtil.isEmpty(token)) {
            throw new JlmCustomException(JlmExceptionEnum.REQUEST_PARAM_ERROR.getResultCode(), "令牌信息错误，请重新登录.");
        }
        return prefixToken + token;
    }*/

    /**
     * 生成19位流水号
     *
     * @return 流水号
     */
    public static String getOrderNo() {
        Snowflake snowflake = IdUtil.getSnowflake(0, 0);
        return snowflake.nextIdStr();
    }

    /**
     * 当前日时 2月25日
     *
     * @return 返回今日时间
     */
    public static String nowDayTime() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM月dd日");
        return dtf.format(LocalDateTime.now());
    }

    /**
     * 做屏蔽词判断和处理
     *
     * @param chatMessages 待检测字符串
     * @return 处理结果
     */
    /*public static String dealSensitiveWords(String chatMessages) {
        return SensitiveWordBs.newInstance().replace(chatMessages, '*');
    }*/

    /**
     * 阿拉伯数字转中文数字
     *
     * @param number 阿拉伯数字
     * @return 中文数字
     */
    public static String numberToChinese(long number) {
        String[] units = {"", "十", "百", "千", "万", "十万", "百万", "千万", "亿", "十亿", "百亿", "千亿"};
        String[] digits = {"零", "一", "二", "三", "四", "五", "六", "七", "八", "九"};
        String numStr = String.valueOf(number);
        int length = numStr.length();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int digit = numStr.charAt(i) - '0';
            int unit = length - i - 1;
            sb.append(digits[digit]);
            if (digit != 0) {
                sb.append(units[unit]);
            } else if (unit == 4 || unit == 8) {
                sb.append(units[unit]);
            }
        }
        String result = sb.toString();
        if (result.startsWith(CHINES_NUMBER)) {
            result = result.substring(1);
        }
        return result;
    }

    /**
     * 构建文件下载路径
     *
     * @param studentId   用户uuid
     * @param groupId    题卷id
     * @param status     题卷状态
     * @param objectName 对象名
     * @return 文件下载路径
     */
    public static String buildFileDownloadUrl(String download, Long studentId, Long groupId, String status, String objectName) {
        log.info("文件下载路径：{}", download + "/bucket/download/" + studentId + "/" + groupId + "/" + status + "/" + objectName);
        return download + "/bucket/download/" + studentId + "/" + groupId + "/" + status + "/" + objectName;
    }

    /**
     * minio下载路径
     * * exercise / 09661c97de54475ebc08c9fdc0841bf3 / 164 / off
     *
     * @param bucketName 桶名称
     * @param userUuid   用户uuid
     * @param groupId    题卷id
     * @param status     状态
     * @param objectName 文件名
     * @return 下载路径
     */
    public static String buildMinIoDownloadUrl(String bucketName, String userUuid, Long groupId, String status, String objectName) {
        return DOWNLOAD_MINIO + "/" + bucketName + "/" + userUuid + "/" + groupId + "/" + status + "/" + objectName;
    }

    /**
     * 通过文件路径获取文件名
     *
     * @param fileUrl 文件路径
     * @return 文件全名
     */
    public static String getWordNameByFileUrl(String fileUrl) {
        return fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
    }
}
