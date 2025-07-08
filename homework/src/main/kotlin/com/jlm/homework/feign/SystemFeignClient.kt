package com.jlm.homework.feign

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping

/**
 * 系统服务 Feign 客户端
 * 用于调用若依系统服务的接口
 */
@FeignClient(
    name = "ruoyi-system",
    configuration = [FeignConfiguration::class]
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
    val roles: List<String>? = emptyList(),
    val userInfo: UserInfo? = null,
    val currentRole: String? = null,
)

data class UserInfo(
    val admin: Boolean? = false,
    val userName: String? = null,
    val nickName: String? = null,
    val userUuid: String? = null,
    val userId: Long? = null,
)