package com.jlm.homework.feign;

import com.jlm.homework.entity.SysSchool;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(value = "jlm-educ-collaborator",configuration = FeignConfiguration.class)
public interface SchoolFeginClient {

    @GetMapping("/school/educOrg")
    public List<SysSchool> getInfoByEducOrg(
            @RequestParam("educOrgId") Long educOrgId,
            @RequestParam("schoolType") String schoolType);
}
