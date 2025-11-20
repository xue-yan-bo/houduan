package com.jlm.homework.controller;

import com.jlm.homework.util.TencentcloudUtil;
import com.tencentcloudapi.ocr.v20181119.models.QuestionSplitOCRRequest;
import com.tencentcloudapi.ocr.v20181119.models.QuestionSplitOCRResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name = "腾讯云API调用", description = "腾讯云API调用")
@RestController
@RequestMapping("/api/tencent-cloud")
public class TencentcloudController {
    @PostMapping("/questionSplit")
    @Operation(summary = "试卷切题")
    public String questionSplit(@RequestBody QuestionSplitOCRRequest req){
        return TencentcloudUtil.questionSplit(req);
    }
}
