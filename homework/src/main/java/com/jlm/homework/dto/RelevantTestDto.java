package com.jlm.homework.dto;

import lombok.Data;

@Data
public class RelevantTestDto {
    private String ImageUrl;//图片的 Url 地址
    private String ImageBase64;//图片的 Base64 值
    private Boolean IsPdf;//是否开启PDF识别
    private Integer PdfPageNumber;//需要识别的PDF页面的对应页码
    private Boolean EnableImageCrop;//是否开启切边增强和弯曲矫正,默认为false不开启
    private Boolean EnableOnlyDetectBorder;//是否只返回检测框，默认false
    private Boolean UseNewModel;//是否多模态推理模型
}
