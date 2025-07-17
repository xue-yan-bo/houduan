package com.jlm.homework.feign;

import com.alibaba.nacos.shaded.com.google.gson.JsonObject;
import com.jlm.homework.dto.Result;
import com.jlm.homework.entity.Student;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/*@FeignClient(
        name = "jlm-student",
        configuration = FeignConfiguration.class
        )*/
@FeignClient(value = "jlm-student",configuration = FeignConfiguration.class)
public interface StudentFeginClient {

        @GetMapping("/student/list")
        public Result<Student> getStudentList(
                @RequestParam("pageNum") Integer pageNum,
                @RequestParam("pageSize") Integer pageSize,
                @RequestParam("schoolId") Long schoolId,
                @RequestParam("classesId") Long classesId,
                @RequestParam("studentStatus") Integer studentStatus);
}
