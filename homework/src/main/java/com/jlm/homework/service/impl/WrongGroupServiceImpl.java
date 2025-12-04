package com.jlm.homework.service.impl;

import com.jlm.homework.dto.HtmlProcessContext;
import com.jlm.homework.entity.WrongGroup;
import com.jlm.homework.entity.WrongGroupItem;
import com.jlm.homework.entity.convert.FileConstant;
import com.jlm.homework.entity.convert.PoiConvert;
import com.jlm.homework.entity.convert.StatusType;
import com.jlm.homework.repository.WrongGroupRepository;
import com.jlm.homework.service.IBucketService;
import com.jlm.homework.service.IWrongGroupService;
import com.jlm.homework.util.BasicUtil;
import com.jlm.homework.util.LatexUtil;
import jakarta.annotation.Resource;
import lombok.extern.log4j.Log4j;
import lombok.extern.slf4j.Slf4j;
import com.alibaba.cloud.commons.lang.StringUtils;
import org.apache.http.client.fluent.Request;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Optional;
@Slf4j
@Service
public class WrongGroupServiceImpl implements IWrongGroupService {
    /**
     * html导出word的时候，必须将头信息修改成这个，不然打开word时候是web视图，而不是word的那个文本视图
     */
    public static final String WORD_HEAD = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\r\n" +
            "<html xmlns=\"http://www.w3.org/TR/REC-html40\" xmlns:v=\"urn:schemas-microsoft-com:vml\" xmlns:o=\"urn:schemas-microsoft-com:office:office\" xmlns:w=\"urn:schemas-microsoft-com:office:word\" xmlns:m=\"http://schemas.microsoft.com/office/2004/12/omml\">" +
            "<head><meta name=\"ProgId\" content=\"Word.Document\" /><meta name=\"Generator\" content=\"Microsoft Word 12\" />" +
            "<meta name=\"Originator\" content=\"Microsoft Word 12\" /> " +
            "<!--[if gte mso 9]><xml><w:WordDocument><w:View>Print</w:View></w:WordDocument></xml><[endif]-->" +
            "</head>";


    public static final String WORD_FOOT = "</html>";

    private static final BigDecimal MAX_WORD_IMAGE_WIGHT = BigDecimal.valueOf(550);
    private static final BigDecimal MAX_WORD_IMAGE_HEIGHT = BigDecimal.valueOf(750);
    private static final String DOCX_SUFFIX = ".docx";

    @Resource
    private WrongGroupRepository wrongGroupRepository;

    @Autowired
    private IBucketService bucketService;

    @Override
    public WrongGroup addGroup(WrongGroup wrongGroup) {
        return wrongGroupRepository.save(wrongGroup);
    }

    @Override
    public WrongGroup getById(Long id) {
        Optional<WrongGroup> optional=wrongGroupRepository.findById(id);
        if(optional.isPresent()){
            return optional.get();
        }
        return null;
    }

    @Override
    public WrongGroup update(WrongGroup wrongGroup) {
        return wrongGroupRepository.save(wrongGroup);
    }

    @Override
    public Page<WrongGroup> selectList(Integer pageNum, Integer pageSize, WrongGroup wrongGroup) {
        pageNum = pageNum == null ? 0 : pageNum-1;
        pageSize = pageSize == null ? 10 : pageSize;
        Sort sort = Sort.by(Sort.Direction.DESC, "createTime");
        Pageable pageable;
        pageable = PageRequest.of(pageNum, pageSize, sort);
        return wrongGroupRepository.findAll(Example.of(wrongGroup),pageable);
    }

    @Override
    public void deleteById(Long id) {
        wrongGroupRepository.deleteById(id);
    }

    @Override
    public void toWord(WrongGroup wrongGroup, List<WrongGroupItem> itemList) {
        long startTime = System.currentTimeMillis();

        // 构建唯一文件名称
        String fileName = wrongGroup.getName() + "（" + BasicUtil.nowDayTime() + "）" + DOCX_SUFFIX;

        // 创建请求级别的处理上下文（两级缓存的核心）
        HtmlProcessContext context = new HtmlProcessContext(wrongGroup.getStudentId(), wrongGroup.getId());

        /* 含答案处理（第一次处理，建立缓存） */
        StringBuilder hasAnswerHtmlBuilder = new StringBuilder();
        for (WrongGroupItem item : itemList) {
            if(item!=null&&item.getContent()!=null&&item.getSolution()!=null) {
                hasAnswerHtmlBuilder.append(new String(item.getContent())).append(new String(item.getSolution()));
            }
        }
        String hasAnswerHtml = cleanHtmlWithCache(hasAnswerHtmlBuilder.toString(), context);
        hasAnswerHtml = "错题组卷" + hasAnswerHtml + WORD_FOOT;
        // 生成含答案文档并上传
        InputStream hasAnswerInputStream = new ByteArrayInputStream(hasAnswerHtml.getBytes(StandardCharsets.UTF_8));
        PoiConvert hasAnswerPoiConvert = new PoiConvert(hasAnswerInputStream);
        byte[] onBytes = hasAnswerPoiConvert.convert();
        String onFileUrl = uploadGroupDocx(wrongGroup, onBytes, fileName, StatusType.STATUS_ANSWER_ON);

        /* 无答案处理（复用缓存，避免重复计算） */
        StringBuilder noAnswerHtmlBuilder = new StringBuilder();
        for (WrongGroupItem item : itemList) {
            noAnswerHtmlBuilder.append(new String(item.getContent()));
        }
        String noAnswerHtml = cleanHtmlWithCache(noAnswerHtmlBuilder.toString(), context);
        noAnswerHtml = "错题组卷"  + noAnswerHtml + WORD_FOOT;

        // 生成无答案文档并上传
        InputStream noAnswerInputStream = new ByteArrayInputStream(noAnswerHtml.getBytes(StandardCharsets.UTF_8));
        PoiConvert noAnswerPoiConvert = new PoiConvert(noAnswerInputStream);
        byte[] offBytes = noAnswerPoiConvert.convert();
        String offFileUrl = uploadGroupDocx(wrongGroup, offBytes, fileName, StatusType.STATUS_ANSWER_OFF);

        // 记录缓存统计信息

        long totalTime = System.currentTimeMillis() - startTime;
        log.info("题卷发布完成 [groupId={}], 总耗时={}ms, LaTeX缓存命中率={}/{} ({:.2f}%), 图片缓存命中率={}/{} ({:.2f}%)",
                wrongGroup.getId(), totalTime,
                context.getLatexCacheHits(), context.getLatexCacheTotal(), context.getLatexCacheHitRate() * 100,
                context.getImageCacheHits(), context.getImageCacheTotal(), context.getImageCacheHitRate() * 100);

        wrongGroup.setFileUrlOn(onFileUrl);
        wrongGroup.setFileUrlOff(offFileUrl);
        wrongGroup.setUpdateTime(new Date());
        wrongGroupRepository.save(wrongGroup);
    }


    /**
     * 清理 HTML（带两级缓存优化）
     * 第一级：请求级内存缓存（避免含答案/不含答案文档重复处理）
     * 第二级：Redis 全局缓存（跨请求复用 LaTeX 渲染结果）
     *
     * @param html    HTML 内容
     * @param context 处理上下文（包含缓存）
     * @return 处理后的 HTML
     */
    private String cleanHtmlWithCache(String html, HtmlProcessContext context) {
        Document parse = Jsoup.parse(html);

        /* LaTeX 公式处理（带两级缓存） */
        try {
            for (Element element : parse.select("span[data-value]")) {
                String decodedHtml = element.attr("data-value").replace("\\\"", "");
                if (StringUtils.isNotEmpty(decodedHtml)) {
                    String latexStr = LatexUtil.latexFormat(decodedHtml).replace("\\\\", "\\");
                    context.incrementLatexTotal();

                    // 第一级：请求级内存缓存（最快）
                    String cachedUrl = context.getLatexCache().get(latexStr);
                    if (cachedUrl != null) {
                        context.incrementLatexHits();
                        Element imgElement = new Element("img").attr("src", cachedUrl);
                        element.replaceWith(imgElement);
                        continue;
                    }

                    // 第二级：Redis 全局缓存 + 渲染
                    String imageUrl;

                    // 直接渲染
                    Image image = LatexUtil.latex2Image(latexStr, 24);
                    ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                    ImageIO.write((BufferedImage) image, "png", outStream);
                    byte[] imageData = outStream.toByteArray();
                    imageUrl = bucketService.upload(imageData, FileConstant.MIME_TYPE_PNG,
                            context.getStudentId(), context.getGroupId(), StatusType.OTHER_RESOURCES,
                            BasicUtil.getOrderNo() + ".png");


                    // 存入请求级缓存
                    context.getLatexCache().put(latexStr, imageUrl);

                    // 替换元素
                    Element imgElement = new Element("img").attr("src", imageUrl);
                    element.replaceWith(imgElement);
                }
            }
        } catch (Exception e) {
            log.error("用户{}发布题卷{}时，LaTeX处理异常", context.getStudentId(), context.getGroupId(), e);
        }

        /* 图片处理（带请求级缓存） */
        try {
            for (Element element : parse.select("img")) {
                String originalUrl = element.attr("src").replace("\\\"", "");
                context.incrementImageTotal();

                // 第一级：请求级内存缓存
                HtmlProcessContext.ImageProcessResult cachedResult = context.getImageCache().get(originalUrl);
                if (cachedResult != null) {
                    context.incrementImageHits();
                    Element div = new Element("div");
                    Element img = new Element("img")
                            .attr("src", cachedResult.getUrl())
                            .attr("width", cachedResult.getWidth())
                            .attr("height", cachedResult.getHeight());
                    div.appendChild(img);
                    element.replaceWith(div);
                    continue;
                }

                // 缓存未命中，处理图片
                log.debug("图片url: {}", originalUrl);
                String url = originalUrl.replace("124.165.206.34:20017", "172.31.100.2:80")
                        .replace("124.165.206.34:20029", "172.31.100.8");

                InputStream image = Request.Get(url).execute().returnContent().asStream();
                BufferedImage sourceImg = ImageIO.read(image);
                BigDecimal width = BigDecimal.valueOf(sourceImg.getWidth());
                BigDecimal height = BigDecimal.valueOf(sourceImg.getHeight());

                // 调整尺寸
                if (width.compareTo(MAX_WORD_IMAGE_WIGHT) > 0) {
                    height = height.divide(width.divide(MAX_WORD_IMAGE_WIGHT, 1, RoundingMode.CEILING),
                            RoundingMode.CEILING);
                    width = MAX_WORD_IMAGE_WIGHT;
                }
                if (height.compareTo(MAX_WORD_IMAGE_HEIGHT) > 0) {
                    width = width.divide(height.divide(MAX_WORD_IMAGE_HEIGHT, 1, RoundingMode.CEILING),
                            RoundingMode.CEILING);
                    height = MAX_WORD_IMAGE_HEIGHT;
                }

                // 存入请求级缓存
                context.getImageCache().put(originalUrl,
                        new HtmlProcessContext.ImageProcessResult(url, width.toString(), height.toString()));

                // 替换元素
                Element div = new Element("div");
                Element img = new Element("img")
                        .attr("src", url)
                        .attr("width", width.toString())
                        .attr("height", height.toString());
                div.appendChild(img);
                element.replaceWith(div);
            }
        } catch (Exception e) {
            log.error("用户{}发布题卷{}时，图片处理异常", context.getStudentId(), context.getGroupId(), e);
        }

        return parse.select("body").outerHtml();
    }

    /**
     * 清理 HTML（旧版本，保留兼容性）
     *
     * @deprecated 使用 {@link #cleanHtmlWithCache(String, HtmlProcessContext)} 替代
     */
    @Deprecated
    private String cleanHtml(String html, Long studentId, Long groupId) {
        // 创建临时上下文，使用新方法
        HtmlProcessContext context = new HtmlProcessContext(studentId, groupId);
        return cleanHtmlWithCache(html, context);
    }
    private String uploadGroupDocx(WrongGroup group, byte[] bytes, String fileName, String status) {
        try {
            return bucketService.upload(bytes, null, group.getStudentId(), group.getId(), status, fileName);
        } catch (Exception e) {
            log.error("上传题卷{}时，上传文件异常", group.getId(), e);
            throw new RuntimeException( "上传题卷" + group.getId() + "时，上传文件异常");
        }
    }
}
