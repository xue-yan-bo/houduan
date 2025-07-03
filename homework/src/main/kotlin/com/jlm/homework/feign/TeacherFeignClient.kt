package com.jlm.homework.feign

import org.springframework.cloud.openfeign.FeignClient

@FeignClient("jlm-schoolworkers")
interface TeacherFeignClient {
}