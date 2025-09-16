package com.jlm.homework.feign;

import com.jlm.homework.dto.Result;
import com.jlm.homework.entity.Student;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/*@FeignClient(
        name = "jlm-student",
        configuration = FeignConfiguration.class
        )*/
@FeignClient(value = "jlm-student",configuration = com.jlm.homework.feign.FeignConfiguration.class)
public interface StudentFeginClient {

        @GetMapping("/student/list")
        public Result<Student> getStudentList(
                @RequestParam("pageNum") Integer pageNum,
                @RequestParam("pageSize") Integer pageSize,
                @RequestParam("schoolId") Long schoolId,
                @RequestParam("gradeId") Long gradeId,
                @RequestParam("classesId") Long classesId,
                @RequestParam("studentStatus") String studentStatus);
}
