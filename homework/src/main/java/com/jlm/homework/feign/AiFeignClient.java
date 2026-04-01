package com.jlm.homework.feign;

import com.jlm.homework.dto.Result;
import com.jlm.homework.dto.SimilarityRequestDto;
import com.jlm.homework.dto.SimilarityResultDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "ai-server", path = "/ai")
public interface AiFeignClient {

    @PostMapping("/analyzeImage")
    Result<String> analyzeImage(@RequestParam("imageUrl") String imageUrl);

    @PostMapping("/checkSimilarity")
    Result<SimilarityResultDto> checkSimilarity(@RequestBody SimilarityRequestDto request);
}
