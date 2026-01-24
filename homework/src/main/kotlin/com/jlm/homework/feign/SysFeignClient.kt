package com.jlm.homework.feign

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping

@FeignClient(
    name = "jlm-system",
    configuration = [FeignConfiguration::class],
)
interface SysFeignClient {

    @GetMapping("/login-info/school")
    fun getCurrentSchoolInfo(): Map<String, Any>?


}


data class School(
    val schoolId: Long,
    val schoolName: String,
    val style: String
)