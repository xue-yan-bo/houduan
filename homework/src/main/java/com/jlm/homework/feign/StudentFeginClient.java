package com.jlm.homework.feign;

import com.jlm.homework.entity.Student;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/*@FeignClient(
        name = "jlm-student",
        configuration = FeignConfiguration.class
        )*/
@FeignClient(value = "jlm-student")
public interface StudentFeginClient {

        @GetMapping("/student/student/list")
        public List<Student> getStudentList(Integer pageNum, Integer pageSize, Long classesId);
}
