package com.jlm.homework.feign;

import com.jlm.homework.dto.Result;
import com.jlm.homework.dto.ResultDto;
import com.jlm.homework.entity.Student;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/*@FeignClient(
        name = "jlm-student",
        configuration = FeignConfiguration.class
        )*/
@FeignClient(value = "jlm-student",configuration = FeignConfiguration.class)
public interface StudentFeignClient {

        @GetMapping("/student/list")
        public Result<Student> getStudentList(
                @RequestParam("pageNum") Integer pageNum,
                @RequestParam("pageSize") Integer pageSize,
                @RequestParam("schoolId") Long schoolId,
                @RequestParam("gradeId") Long gradeId,
                @RequestParam("classesId") Long classesId,
                @RequestParam("studentStatus") String studentStatus);
        @GetMapping("/student/getStudentInfo/{studentId}")
        public ResultDto<Student> getStudentInfo(@PathVariable("studentId") Long studentId);
}
