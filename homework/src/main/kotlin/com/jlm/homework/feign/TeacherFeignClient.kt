package com.jlm.homework.feign

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam

@FeignClient("jlm-schoolworkers")
interface TeacherFeignClient {


    /**
     * 根据 userUuid获取教职工信息
     */
    @GetMapping("/teacher/getTeacherByUserUuid")
    fun getTeacherByUserUuid(@RequestParam("userUuid") userUuid: String): Teacher?


}


data class Teacher(
    val userUuid: String? = null,
    val name: String? = null,
)


