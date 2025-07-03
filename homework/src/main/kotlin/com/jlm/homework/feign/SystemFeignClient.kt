package com.jlm.homework.feign

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping

@FeignClient(
    name = "ruoyi-system",
    configuration = [FeignConfiguration::class],
)
interface SystemFeignClient {

    /**
     * 获取登录用户信息
     * @return 登录用户信息
     */
    @GetMapping("user/getInfo?roleKey=")
    fun loginUserInfo(): LoginUserInfo?
}


data class LoginUserInfo(
    val roles: List<String>,
    val userInfo: UserInfo,
    val currentRole: String,
)

data class UserInfo(
    val admin: Boolean,
    val userName: String,
    val nickName: String,
    val userUuid: String,
    val userId: Long,
)