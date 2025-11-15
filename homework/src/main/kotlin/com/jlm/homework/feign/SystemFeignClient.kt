package com.jlm.homework.feign

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

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

    @GetMapping("/user/info/{username}")
    fun info(@PathVariable("username") username: String) :Result
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

data class LoginUser(
    val token: String? = null,
    val username: String? = null,
    val userid: Long? = null,
    val currentRole: String? = null,
    val sysUser: UserInfo? = null,
    val loginTime: Long? = null,
)

data class Result(
    val code: Integer? = null,
    val message: String? = null,
    val data: LoginUser? = null
)