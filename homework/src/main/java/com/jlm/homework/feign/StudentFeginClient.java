package com.jlm.homework.feign;

import com.jlm.homework.entity.Student;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

/*@FeignClient(
        name = "jlm-student",
        configuration = FeignConfiguration.class
        )*/
@FeignClient(value = "jlm-student",configuration = FeignConfiguration.class)
public interface StudentFeginClient {

        @GetMapping("/student/student/list")
        public List<Student> getStudentList(
                @RequestParam("pageNum") Integer pageNum,
                @RequestParam("pageSize") Integer pageSize,
                @RequestParam("classesId") Long classesId);
}
