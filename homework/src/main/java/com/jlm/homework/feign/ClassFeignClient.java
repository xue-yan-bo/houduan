package com.jlm.homework.feign;


import com.jlm.homework.entity.Classes;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "jlm-classes",configuration = FeignConfiguration.class)
public interface ClassFeignClient {
    /**
     * 根据班级ID获取班级信息带班主任
     * @param classesId
     * @return
     */
    @GetMapping("/classes/getClasses/{classesId}")
    public Classes getClasses(@PathVariable("classesId") Long classesId);
}

