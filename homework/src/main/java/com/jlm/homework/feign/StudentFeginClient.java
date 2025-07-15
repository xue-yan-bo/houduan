package com.jlm.homework.feign;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(
        name = "jlm-student",
        configuration = FeignConfiguration.class
        )
public interface StudentFeginClient {
}
