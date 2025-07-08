package com.jlm.homework.service

import com.jlm.homework.feign.SystemFeignClient
import com.jlm.homework.feign.TeacherFeignClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 用户服务类
 * 提供获取当前登录用户信息的功能
 */
@Service
class UserService(
    private val systemFeignClient: SystemFeignClient,
    private val teacherFeignClient: TeacherFeignClient
) {

    private val logger = LoggerFactory.getLogger(UserService::class.java)

    /**
     * 获取当前登录用户ID
     * @return 用户ID，如果获取失败返回默认用户ID
     */
    fun getCurrentUserId(): Long? {
        return try {
            val loginUserInfo = systemFeignClient.loginUserInfo()
            val userId = loginUserInfo?.userInfo?.userId
            if (userId != null && userId > 0) {
                logger.info("获取当前用户ID: {}", userId)
                userId
            } else {
                logger.warn("获取到的用户ID无效: {}", userId)
                // 返回默认用户ID，避免阻塞业务流程
                getDefaultUserId()
            }
        } catch (e: Exception) {
            logger.warn("获取当前用户ID失败: {}", e.message)
            // 返回默认用户ID，避免阻塞业务流程
            getDefaultUserId()
        }
    }

    /**
     * 获取默认用户ID
     * 当无法获取当前用户信息时使用
     */
    private fun getDefaultUserId(): Long {
        // 可以从配置文件读取，或者使用固定值
        val defaultUserId = 1000L
        logger.info("使用默认用户ID: {}", defaultUserId)
        return defaultUserId
    }

    /**
     * 安全获取当前用户ID
     * 提供更好的容错性，确保不会因为远程服务问题而阻塞业务
     */
    fun getCurrentUserIdSafely(): Long {
        return try {
            getCurrentUserId() ?: getDefaultUserId()
        } catch (e: Exception) {
            logger.warn("安全获取用户ID失败，使用默认值: {}", e.message)
            getDefaultUserId()
        }
    }

    /**
     * 获取当前登录用户信息
     * @return 用户信息，如果获取失败返回null
     */
    fun getCurrentUserInfo(): CurrentUserInfo? {
        return try {
            val loginUserInfo = systemFeignClient.loginUserInfo()
            if (loginUserInfo?.userInfo != null) {
                val userInfo = loginUserInfo.userInfo!!

                // 尝试获取教师信息
                val teacher = try {
                    userInfo.userUuid?.let { uuid ->
                        teacherFeignClient.getTeacherByUserUuid(uuid)
                    }
                } catch (e: Exception) {
                    logger.debug("获取教师信息失败，用户可能不是教师: {}", userInfo.userUuid)
                    null
                }

                CurrentUserInfo(
                    userId = userInfo.userId ?: 0L,
                    userName = userInfo.userName ?: "",
                    nickName = userInfo.nickName ?: "",
                    userUuid = userInfo.userUuid ?: "",
                    isAdmin = userInfo.admin ?: false,
                    roles = loginUserInfo.roles ?: emptyList(),
                    currentRole = loginUserInfo.currentRole ?: "",
                    teacherName = teacher?.name
                )
            } else {
                logger.warn("未获取到登录用户信息")
                null
            }
        } catch (e: Exception) {
            logger.error("获取当前用户信息失败: {}", e.message)
            null
        }
    }

    /**
     * 检查当前用户是否为教师
     * @return true如果是教师，false如果不是或获取失败
     */
    fun isCurrentUserTeacher(): Boolean {
        return try {
            val userInfo = getCurrentUserInfo()
            userInfo?.teacherName != null
        } catch (e: Exception) {
            logger.warn("检查用户教师身份失败", e)
            false
        }
    }
}

/**
 * 当前用户信息数据类
 */
data class CurrentUserInfo(
    val userId: Long,
    val userName: String,
    val nickName: String,
    val userUuid: String,
    val isAdmin: Boolean,
    val roles: List<String>,
    val currentRole: String,
    val teacherName: String? = null
)
