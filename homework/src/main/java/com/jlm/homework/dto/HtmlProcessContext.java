package com.jlm.homework.dto;

import lombok.Data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * HTML 处理上下文（请求级缓存）
 * 用于在单次请求中缓存 LaTeX 和图片处理结果，避免重复计算
 *
 * @author QingYang
 * @version 1.0
 * @date 2024/10/21
 */
@Data
public class HtmlProcessContext {

    /**
     * LaTeX 公式处理缓存: LaTeX字符串 -> 图片URL
     */
    private final Map<String, String> latexCache = new ConcurrentHashMap<>();

    /**
     * 图片处理缓存: 原始图片URL -> 处理结果（格式: url|width|height）
     */
    private final Map<String, ImageProcessResult> imageCache = new ConcurrentHashMap<>();

    /**
     * 用户 UUID
     */
    private Long studentId;

    /**
     * 题卷 ID
     */
    private final Long groupId;

    /**
     * LaTeX 缓存命中次数
     */
    private final AtomicInteger latexCacheHits = new AtomicInteger(0);

    /**
     * LaTeX 总处理次数
     */
    private final AtomicInteger latexCacheTotal = new AtomicInteger(0);

    /**
     * 图片缓存命中次数
     */
    private final AtomicInteger imageCacheHits = new AtomicInteger(0);

    /**
     * 图片总处理次数
     */
    private final AtomicInteger imageCacheTotal = new AtomicInteger(0);

    public HtmlProcessContext(Long studentId, Long groupId) {
        this.studentId = studentId;
        this.groupId = groupId;
    }

    public void incrementLatexHits() {
        latexCacheHits.incrementAndGet();
    }

    public void incrementLatexTotal() {
        latexCacheTotal.incrementAndGet();
    }

    public void incrementImageHits() {
        imageCacheHits.incrementAndGet();
    }

    public void incrementImageTotal() {
        imageCacheTotal.incrementAndGet();
    }

    /**
     * 获取 LaTeX 缓存命中率
     */
    public double getLatexCacheHitRate() {
        int total = latexCacheTotal.get();
        return total > 0 ? (double) latexCacheHits.get() / total : 0.0;
    }

    /**
     * 获取图片缓存命中率
     */
    public double getImageCacheHitRate() {
        int total = imageCacheTotal.get();
        return total > 0 ? (double) imageCacheHits.get() / total : 0.0;
    }

    /**
     * 图片处理结果
     */
    @Data
    public static class ImageProcessResult {
        private final String url;
        private final String width;
        private final String height;

        public ImageProcessResult(String url, String width, String height) {
            this.url = url;
            this.width = width;
            this.height = height;
        }
    }
}

